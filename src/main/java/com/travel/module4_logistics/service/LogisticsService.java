package com.travel.module4_logistics.service;

import com.travel.module4_logistics.entity.*;
import com.travel.module4_logistics.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

@Service
public class LogisticsService {
    private final SupplierRepository suppliers;
    private final HotelBookingRepository hotels;
    private final TransportRepository transports;
    private final GuideRepository guides;
    private final AssignmentRepository assignments;
    private final SupplierPaymentRepository payments;
    private final DepartureRepository departures;

    public LogisticsService(SupplierRepository suppliers, HotelBookingRepository hotels,
                            TransportRepository transports, GuideRepository guides,
                            AssignmentRepository assignments, SupplierPaymentRepository payments,
                            DepartureRepository departures) {
        this.suppliers = suppliers;
        this.hotels = hotels;
        this.transports = transports;
        this.guides = guides;
        this.assignments = assignments;
        this.payments = payments;
        this.departures = departures;
    }

    @Transactional
    public void addDeparture(String reference, String packageName, String destination, LocalDate start, LocalDate end) {
        requireText(reference, "Departure reference");
        requireText(packageName, "Package name");
        requireText(destination, "Destination");
        checkDates(start, end);
        if (reference.trim().length() > 20 || packageName.trim().length() > 100 || destination.trim().length() > 100)
            badRequest("Departure details exceed the allowed length");
        if (departures.existsByReferenceIgnoreCase(reference.trim())) badRequest("This departure reference already exists");
        departures.save(new Departure(reference.trim(), packageName.trim(), destination.trim(), start, end));
    }

    @Transactional
    public void addSupplier(String name, String type, String phone, LocalDate start, LocalDate end, Integer rooms) {
        requireText(name, "Supplier name");
        requireType(type, "HOTEL", "TRANSPORT", "GUIDE");
        checkDates(start, end);
        if (type.equals("HOTEL") && (rooms == null || rooms < 1)) {
            badRequest("Enter the hotel's room capacity");
        }
        suppliers.save(new Supplier(name.trim(), type, phone, start, end, type.equals("HOTEL") ? rooms : null));
    }

    @Transactional(readOnly = true)
    public Supplier getSupplier(Integer id) {
        return suppliers.findById(id).orElseThrow(() -> missing("Supplier not found"));
    }

    @Transactional
    public void updateSupplier(Integer id, String name, String type, String phone,
                               LocalDate start, LocalDate end, Integer rooms) {
        Supplier supplier = suppliers.findForAllocation(id)
                .orElseThrow(() -> missing("Supplier not found"));
        requireText(name, "Supplier name");
        requireType(type, "HOTEL", "TRANSPORT", "GUIDE");
        checkDates(start, end);
        if (name.trim().length() > 100 || (phone != null && phone.length() > 30))
            badRequest("Supplier name or phone exceeds the allowed length");
        if (type.equals("HOTEL") && (rooms == null || rooms < 1))
            badRequest("Enter the hotel's room capacity");

        List<HotelBooking> linkedHotels = hotels.findAll().stream()
                .filter(h -> h.getSupplier().getId().equals(id)).toList();
        List<Transport> linkedTransports = transports.findAll().stream()
                .filter(t -> t.getSupplier().getId().equals(id)).toList();
        boolean hasGuides = guides.findAll().stream()
                .anyMatch(g -> g.getSupplier().getId().equals(id));
        boolean hasPayments = payments.findAll().stream()
                .anyMatch(p -> p.getSupplier().getId().equals(id));
        if (!supplier.getType().equals(type) &&
                (!linkedHotels.isEmpty() || !linkedTransports.isEmpty() || hasGuides || hasPayments))
            badRequest("Cannot change service type while this supplier has linked records");

        // Validate before changing the managed entity, so rejected edits leave it intact.
        for (HotelBooking booking : linkedHotels) {
            checkUpdatedContract(start, end, booking.getCheckIn(), booking.getCheckOut());
            // Check each arrival date: occupancy can only increase when a stay starts.
            long occupied = linkedHotels.stream()
                    .filter(h -> !h.getCheckIn().isAfter(booking.getCheckIn())
                            && h.getCheckOut().isAfter(booking.getCheckIn()))
                    .mapToLong(HotelBooking::getRooms).sum();
            if (rooms == null || occupied > rooms)
                badRequest("Room limit is below the rooms already allocated on " + booking.getCheckIn());
        }
        for (Transport transport : linkedTransports)
            checkUpdatedContract(start, end, transport.getServiceDate(), transport.getServiceDate());
        for (Assignment assignment : assignments.findAll()) {
            if (assignment.getGuide().getSupplier().getId().equals(id))
                checkUpdatedContract(start, end, assignment.getStartDate(), assignment.getEndDate());
        }
        supplier.updateDetails(name.trim(), type, phone, start, end, type.equals("HOTEL") ? rooms : null);
        suppliers.save(supplier);
    }

    private void checkUpdatedContract(LocalDate start, LocalDate end,
                                      LocalDate serviceStart, LocalDate serviceEnd) {
        if (serviceStart.isBefore(start) || serviceEnd.isAfter(end))
            badRequest("Contract dates must cover this supplier's existing service arrangements");
    }

    @Transactional
    public void bookHotel(Integer supplierId, Integer departureId, LocalDate start, LocalDate end,
                          Integer rooms, BigDecimal cost) {
        Supplier hotel = lockedSupplier(supplierId, "HOTEL");
        Departure departure = departure(departureId);
        checkTripDates(departure, start, end);
        if (!end.isAfter(start)) badRequest("Hotel check-out must be after check-in");
        checkContract(hotel, start, end);
        if (rooms == null || rooms < 1 || !positiveOrZero(cost)) badRequest("Enter rooms and a valid total cost");

        int reserved = hotels.findBySupplier_IdAndCheckInLessThanAndCheckOutGreaterThan(hotel.getId(), end, start)
                .stream().mapToInt(HotelBooking::getRooms).sum();
        if (reserved + rooms > hotel.getRoomCapacity()) badRequest("Not enough rooms available for those dates");
        hotels.save(new HotelBooking(hotel, departure, start, end, rooms, cost));
    }

    @Transactional
    public void addTransport(Integer supplierId, Integer departureId, String mode, String reference,
                             LocalDate date, Integer seats, BigDecimal cost) {
        Supplier supplier = lockedSupplier(supplierId, "TRANSPORT");
        Departure departure = departure(departureId);
        requireType(mode, "FLIGHT", "COACH", "TRAIN", "VEHICLE");
        requireText(reference, "Vehicle or service reference");
        checkTripDates(departure, date, date);
        checkContract(supplier, date, date);
        if (seats == null || seats < 1 || !positiveOrZero(cost)) badRequest("Enter seats and a valid total cost");
        if (transports.existsBySupplier_IdAndVehicleReferenceIgnoreCaseAndServiceDate(supplierId, reference.trim(), date)) {
            badRequest("This vehicle or service is already allocated on that date");
        }
        transports.save(new Transport(supplier, departure, mode, reference.trim(), date, seats, cost));
    }

    @Transactional
    public void addGuide(Integer supplierId, String name, String role, String phone, BigDecimal dailyRate) {
        Supplier supplier = lockedSupplier(supplierId, "GUIDE");
        requireText(name, "Guide or driver name");
        requireType(role, "GUIDE", "DRIVER");
        if (!positiveOrZero(dailyRate)) badRequest("Enter a valid daily rate");
        guides.save(new Guide(supplier, name.trim(), role, phone, dailyRate));
    }

    @Transactional
    public void assignGuide(Integer guideId, Integer departureId, LocalDate start, LocalDate end) {
        Guide guide = guides.findById(guideId).orElseThrow(() -> missing("Guide or driver not found"));
        // Lock the supplier so two assignments for its staff cannot pass the availability check together.
        Supplier supplier = lockedSupplier(guide.getSupplier().getId(), "GUIDE");
        Departure departure = departure(departureId);
        checkTripDates(departure, start, end);
        checkContract(supplier, start, end);
        if (assignments.existsByGuide_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(guideId, end, start)) {
            badRequest("This guide or driver is already assigned during those dates");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        assignments.save(new Assignment(departure, guide, start, end,
                guide.getDailyRate().multiply(BigDecimal.valueOf(days))));
    }

    @Transactional
    public void recordPayment(Integer supplierId, Integer departureId, LocalDate date,
                              BigDecimal amount, String reference) {
        Supplier supplier = suppliers.findForAllocation(supplierId).orElseThrow(() -> missing("Supplier not found"));
        Departure departure = departure(departureId);
        if (date == null || !positive(amount)) badRequest("Enter a date and an amount above zero");
        BigDecimal due = costForSupplier(departureId, supplierId).subtract(paidForSupplier(departureId, supplierId));
        if (amount.compareTo(due) > 0) badRequest("Payment exceeds the outstanding supplier cost");
        payments.save(new SupplierPayment(supplier, departure, date, amount, reference));
    }

    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers() { return suppliers.findAllByOrderByNameAsc(); }
    @Transactional(readOnly = true)
    public List<Departure> listDepartures() { return departures.findAllByOrderByStartDateAsc(); }
    @Transactional(readOnly = true)
    public List<HotelBooking> listHotels() { return hotels.findAllByOrderByCheckInDesc(); }
    @Transactional(readOnly = true)
    public List<Transport> listTransports() { return transports.findAllByOrderByServiceDateDesc(); }
    @Transactional(readOnly = true)
    public List<Guide> listGuides() { return guides.findAllByOrderByNameAsc(); }
    @Transactional(readOnly = true)
    public List<Assignment> listAssignments() { return assignments.findAllByOrderByStartDateDesc(); }
    @Transactional(readOnly = true)
    public List<SupplierPayment> listPayments() { return payments.findAllByOrderByPaymentDateDesc(); }

    @Transactional(readOnly = true)
    public BigDecimal costForDeparture(Integer departureId) {
        departure(departureId);
        return hotels.findByDeparture_Id(departureId).stream().map(HotelBooking::getCost).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(transports.findByDeparture_Id(departureId).stream().map(Transport::getCost).reduce(BigDecimal.ZERO, BigDecimal::add))
                .add(assignments.findByDeparture_Id(departureId).stream().map(Assignment::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Transactional(readOnly = true)
    public BigDecimal paidForDeparture(Integer departureId) {
        return payments.findByDeparture_Id(departureId).stream().map(SupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal costForSupplier(Integer departureId, Integer supplierId) {
        BigDecimal hotelCost = hotels.findByDeparture_Id(departureId).stream()
                .filter(h -> h.getSupplier().getId().equals(supplierId)).map(HotelBooking::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal transportCost = transports.findByDeparture_Id(departureId).stream()
                .filter(t -> t.getSupplier().getId().equals(supplierId)).map(Transport::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal staffCost = assignments.findByDeparture_Id(departureId).stream()
                .filter(a -> a.getGuide().getSupplier().getId().equals(supplierId)).map(Assignment::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return hotelCost.add(transportCost).add(staffCost);
    }

    private BigDecimal paidForSupplier(Integer departureId, Integer supplierId) {
        return payments.findByDeparture_Id(departureId).stream()
                .filter(p -> p.getSupplier().getId().equals(supplierId)).map(SupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> editValues(String page, Integer id) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        if (page.equals("departures")) {
            Departure record = departures.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("reference", record.getReference());
            values.put("packageName", record.getPackageName());
            values.put("destination", record.getDestination());
            values.put("startDate", record.getStartDate());
            values.put("endDate", record.getEndDate());
            return values;
        }
        if (page.equals("hotels")) {
            HotelBooking record = hotels.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("supplierId", record.getSupplier().getId());
            values.put("departureId", record.getDeparture().getId());
            values.put("checkIn", record.getCheckIn());
            values.put("checkOut", record.getCheckOut());
            values.put("rooms", record.getRooms());
            values.put("cost", record.getCost());
            return values;
        }
        if (page.equals("transports")) {
            Transport record = transports.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("supplierId", record.getSupplier().getId());
            values.put("departureId", record.getDeparture().getId());
            values.put("mode", record.getMode());
            values.put("reference", record.getVehicleReference());
            values.put("serviceDate", record.getServiceDate());
            values.put("seats", record.getSeats());
            values.put("cost", record.getCost());
            return values;
        }
        if (page.equals("guides")) {
            Guide record = guides.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("supplierId", record.getSupplier().getId());
            values.put("name", record.getName());
            values.put("role", record.getRole());
            values.put("phone", record.getPhone());
            values.put("dailyRate", record.getDailyRate());
            return values;
        }
        if (page.equals("assignments")) {
            Assignment record = assignments.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("guideId", record.getGuide().getId());
            values.put("departureId", record.getDeparture().getId());
            values.put("startDate", record.getStartDate());
            values.put("endDate", record.getEndDate());
            return values;
        }
        if (page.equals("payments")) {
            SupplierPayment record = payments.findById(id).orElseThrow(() -> missing("Record not found"));
            values.put("supplierId", record.getSupplier().getId());
            values.put("departureId", record.getDeparture().getId());
            values.put("paymentDate", record.getPaymentDate());
            values.put("amount", record.getAmount());
            values.put("reference", record.getReference());
            return values;
        }
        throw missing("Page not found");
    }

    @Transactional
    public void updateRecord(String page, Integer id, Map<String, String> values) {
        if (!Set.of("departures", "hotels", "transports", "guides", "assignments", "payments").contains(page))
            throw missing("Page not found");
        // Use the same supplier locks as allocation and payment creation.
        suppliers.findAll().stream().map(Supplier::getId).sorted()
                .forEach(supplierId -> suppliers.findForAllocation(supplierId)
                        .orElseThrow(() -> missing("Supplier not found")));
        List<Departure> allDepartures = departures.findAll();
        List<HotelBooking> allHotels = hotels.findAll();
        List<Transport> allTransports = transports.findAll();
        List<Guide> allGuides = guides.findAll();
        List<Assignment> allAssignments = assignments.findAll();
        List<SupplierPayment> allPayments = payments.findAll();
        try {
            if (page.equals("departures")) {
                Departure record = departures.findById(id).orElseThrow(() -> missing("Record not found"));
                record.updateDetails(text(values, "reference", 20), text(values, "packageName", 100), text(values, "destination", 100), date(values, "startDate"), date(values, "endDate"));
            }
            if (page.equals("hotels")) {
                HotelBooking record = hotels.findById(id).orElseThrow(() -> missing("Record not found"));
                record.updateDetails(lockedSupplier(number(values, "supplierId"), "HOTEL"), departure(number(values, "departureId")), date(values, "checkIn"), date(values, "checkOut"), number(values, "rooms"), money(values, "cost"));
            }
            if (page.equals("transports")) {
                Transport record = transports.findById(id).orElseThrow(() -> missing("Record not found"));
                record.updateDetails(lockedSupplier(number(values, "supplierId"), "TRANSPORT"), departure(number(values, "departureId")), text(values, "mode", 20), text(values, "reference", 60), date(values, "serviceDate"), number(values, "seats"), money(values, "cost"));
            }
            if (page.equals("guides")) {
                Guide record = guides.findById(id).orElseThrow(() -> missing("Record not found"));
                record.updateDetails(lockedSupplier(number(values, "supplierId"), "GUIDE"), text(values, "name", 100), text(values, "role", 20), optionalText(values, "phone", 30), money(values, "dailyRate"));
            }
            if (page.equals("assignments")) {
                Assignment record = assignments.findById(id).orElseThrow(() -> missing("Record not found"));
                Guide person = guides.findById(number(values, "guideId")).orElseThrow(() -> missing("Person not found"));
                LocalDate start = date(values, "startDate"), end = date(values, "endDate");
                checkDates(start, end);
                record.updateDetails(departure(number(values, "departureId")), person, start, end, person.getDailyRate().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1)));
            }
            if (page.equals("payments")) {
                SupplierPayment record = payments.findById(id).orElseThrow(() -> missing("Record not found"));
                record.updateDetails(suppliers.findForAllocation(number(values, "supplierId")).orElseThrow(() -> missing("Supplier not found")), departure(number(values, "departureId")), date(values, "paymentDate"), money(values, "amount"), optionalText(values, "reference", 60));
            }
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException error) {
            badRequest("Enter valid dates, whole numbers and amounts");
        }
        // No further repository queries: validate the proposed managed state before commit.
        for (Departure d : allDepartures) {
            checkDates(d.getStartDate(), d.getEndDate());
            if (allDepartures.stream().anyMatch(other -> !other.getId().equals(d.getId())
                    && other.getReference().equalsIgnoreCase(d.getReference())))
                badRequest("This departure reference already exists");
        }
        for (HotelBooking h : allHotels) {
            checkTripDates(h.getDeparture(), h.getCheckIn(), h.getCheckOut());
            checkContract(h.getSupplier(), h.getCheckIn(), h.getCheckOut());
            if (!h.getCheckOut().isAfter(h.getCheckIn()) || h.getRooms() < 1 || !positiveOrZero(h.getCost()))
                badRequest("Check hotel dates, rooms and total cost");
            long used = allHotels.stream().filter(other -> other.getSupplier().getId().equals(h.getSupplier().getId())
                    && !other.getCheckIn().isAfter(h.getCheckIn()) && other.getCheckOut().isAfter(h.getCheckIn()))
                    .mapToLong(HotelBooking::getRooms).sum();
            if (used > h.getSupplier().getRoomCapacity()) badRequest("Not enough hotel rooms on " + h.getCheckIn());
        }
        for (Transport t : allTransports) {
            requireType(t.getMode(), "FLIGHT", "COACH", "TRAIN", "VEHICLE");
            checkTripDates(t.getDeparture(), t.getServiceDate(), t.getServiceDate());
            checkContract(t.getSupplier(), t.getServiceDate(), t.getServiceDate());
            if (t.getSeats() < 1 || !positiveOrZero(t.getCost())) badRequest("Check transport seats and cost");
            if (allTransports.stream().anyMatch(other -> !other.getId().equals(t.getId())
                    && other.getSupplier().getId().equals(t.getSupplier().getId())
                    && other.getVehicleReference().equalsIgnoreCase(t.getVehicleReference())
                    && other.getServiceDate().equals(t.getServiceDate())))
                badRequest("This transport service is already allocated on that date");
        }
        for (Guide g : allGuides) {
            requireType(g.getRole(), "GUIDE", "DRIVER");
            if (!positiveOrZero(g.getDailyRate())) badRequest("Enter a valid daily rate");
        }
        for (Assignment a : allAssignments) {
            checkTripDates(a.getDeparture(), a.getStartDate(), a.getEndDate());
            checkContract(a.getGuide().getSupplier(), a.getStartDate(), a.getEndDate());
            if (allAssignments.stream().anyMatch(other -> !other.getId().equals(a.getId())
                    && other.getGuide().getId().equals(a.getGuide().getId())
                    && !other.getStartDate().isAfter(a.getEndDate()) && !other.getEndDate().isBefore(a.getStartDate())))
                badRequest("This person is already assigned during those dates");
        }
        for (SupplierPayment payment : allPayments) {
            if (!positive(payment.getAmount())) badRequest("Payment amount must be above zero");
            Integer dep = payment.getDeparture().getId(), supplierId = payment.getSupplier().getId();
            BigDecimal cost = BigDecimal.ZERO, paid = BigDecimal.ZERO;
            for (HotelBooking h : allHotels)
                if (h.getDeparture().getId().equals(dep) && h.getSupplier().getId().equals(supplierId)) cost = cost.add(h.getCost());
            for (Transport t : allTransports)
                if (t.getDeparture().getId().equals(dep) && t.getSupplier().getId().equals(supplierId)) cost = cost.add(t.getCost());
            for (Assignment a : allAssignments)
                if (a.getDeparture().getId().equals(dep) && a.getGuide().getSupplier().getId().equals(supplierId)) cost = cost.add(a.getCost());
            for (SupplierPayment other : allPayments)
                if (other.getDeparture().getId().equals(dep) && other.getSupplier().getId().equals(supplierId)) paid = paid.add(other.getAmount());
            if (paid.compareTo(cost) > 0) badRequest("This edit would leave supplier payments higher than the recorded cost");
        }
    }

    private String text(Map<String, String> values, String key, int limit) {
        String value = values.get(key);
        requireText(value, key);
        if (value.trim().length() > limit) badRequest(key + " is too long");
        return value.trim();
    }
    private String optionalText(Map<String, String> values, String key, int limit) {
        String value = values.getOrDefault(key, "").trim();
        if (value.length() > limit) badRequest(key + " is too long");
        return value;
    }
    private Integer number(Map<String, String> values, String key) { return Integer.valueOf(text(values, key, 15)); }
    private LocalDate date(Map<String, String> values, String key) { return LocalDate.parse(text(values, key, 10)); }
    private BigDecimal money(Map<String, String> values, String key) {
        BigDecimal amount = new BigDecimal(text(values, key, 30));
        if (amount.scale() > 2 || amount.abs().compareTo(new BigDecimal("99999999.99")) > 0)
            badRequest("Amounts must have at most two decimal places and fit the allowed range");
        return amount;
    }

    @Transactional
    public void deleteRecords(String page, List<Integer> selectedIds) {
        if (!Set.of("departures", "suppliers", "hotels", "transports", "guides", "assignments", "payments").contains(page))
            throw missing("Page not found");
        if (selectedIds == null || selectedIds.isEmpty()) badRequest("Select at least one record to delete");
        Set<Integer> ids = new java.util.HashSet<>(selectedIds);
        if (ids.contains(null)) badRequest("Invalid record selection");
        // Lock in the same order as editing; creations lock these suppliers too.
        suppliers.findAll().stream().map(Supplier::getId).sorted()
                .forEach(id -> suppliers.findForAllocation(id).orElseThrow(() -> missing("Supplier not found")));
        List<Departure> allDepartures = departures.findAll();
        List<Supplier> allSuppliers = suppliers.findAll();
        List<HotelBooking> allHotels = hotels.findAll();
        List<Transport> allTransports = transports.findAll();
        List<Guide> allGuides = guides.findAll();
        List<Assignment> allAssignments = assignments.findAll();
        List<SupplierPayment> allPayments = payments.findAll();
        Set<Integer> existing = new java.util.HashSet<>();
        if (page.equals("departures")) allDepartures.forEach(record -> existing.add(record.getId()));
        if (page.equals("suppliers")) allSuppliers.forEach(record -> existing.add(record.getId()));
        if (page.equals("hotels")) allHotels.forEach(record -> existing.add(record.getId()));
        if (page.equals("transports")) allTransports.forEach(record -> existing.add(record.getId()));
        if (page.equals("guides")) allGuides.forEach(record -> existing.add(record.getId()));
        if (page.equals("assignments")) allAssignments.forEach(record -> existing.add(record.getId()));
        if (page.equals("payments")) allPayments.forEach(record -> existing.add(record.getId()));
        if (!existing.containsAll(ids)) badRequest("A selected record no longer exists. Refresh and try again");
        if (page.equals("departures")) {
            if (allHotels.stream().anyMatch(h -> ids.contains(h.getDeparture().getId()))
                    || allTransports.stream().anyMatch(t -> ids.contains(t.getDeparture().getId()))
                    || allAssignments.stream().anyMatch(a -> ids.contains(a.getDeparture().getId()))
                    || allPayments.stream().anyMatch(p -> ids.contains(p.getDeparture().getId())))
                badRequest("Cannot delete: a selected departure has services, assignments or payments");
        }
        if (page.equals("suppliers")) {
            if (allHotels.stream().anyMatch(h -> ids.contains(h.getSupplier().getId()))
                    || allTransports.stream().anyMatch(t -> ids.contains(t.getSupplier().getId()))
                    || allGuides.stream().anyMatch(g -> ids.contains(g.getSupplier().getId()))
                    || allPayments.stream().anyMatch(p -> ids.contains(p.getSupplier().getId())))
                badRequest("Cannot delete: a selected supplier is used by other records");
        }
        if (page.equals("guides") && allAssignments.stream().anyMatch(a -> ids.contains(a.getGuide().getId())))
            badRequest("Cannot delete: a selected guide or driver has assignments");
        if (Set.of("hotels", "transports", "assignments").contains(page)) {
            // Calculate the combined effect of the entire selection before deleting any row.
            for (SupplierPayment payment : allPayments) {
                Integer dep = payment.getDeparture().getId(), supplierId = payment.getSupplier().getId();
                BigDecimal cost = BigDecimal.ZERO, paid = BigDecimal.ZERO;
                for (HotelBooking h : allHotels)
                    if (!(page.equals("hotels") && ids.contains(h.getId()))
                            && h.getDeparture().getId().equals(dep) && h.getSupplier().getId().equals(supplierId))
                        cost = cost.add(h.getCost());
                for (Transport t : allTransports)
                    if (!(page.equals("transports") && ids.contains(t.getId()))
                            && t.getDeparture().getId().equals(dep) && t.getSupplier().getId().equals(supplierId))
                        cost = cost.add(t.getCost());
                for (Assignment a : allAssignments)
                    if (!(page.equals("assignments") && ids.contains(a.getId()))
                            && a.getDeparture().getId().equals(dep) && a.getGuide().getSupplier().getId().equals(supplierId))
                        cost = cost.add(a.getCost());
                for (SupplierPayment p : allPayments)
                    if (p.getDeparture().getId().equals(dep) && p.getSupplier().getId().equals(supplierId))
                        paid = paid.add(p.getAmount());
                if (paid.compareTo(cost) > 0)
                    badRequest("Cannot delete: supplier payments would exceed the remaining recorded cost");
            }
        }
        // All checks passed. The transaction commits the whole selection or rolls it all back.
        if (page.equals("departures")) departures.deleteAll(allDepartures.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("suppliers")) suppliers.deleteAll(allSuppliers.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("hotels")) hotels.deleteAll(allHotels.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("transports")) transports.deleteAll(allTransports.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("guides")) guides.deleteAll(allGuides.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("assignments")) assignments.deleteAll(allAssignments.stream().filter(record -> ids.contains(record.getId())).toList());
        if (page.equals("payments")) payments.deleteAll(allPayments.stream().filter(record -> ids.contains(record.getId())).toList());
    }

    private Supplier lockedSupplier(Integer id, String type) {
        Supplier supplier = suppliers.findForAllocation(id).orElseThrow(() -> missing("Supplier not found"));
        if (!supplier.getType().equals(type)) badRequest("Select a " + type.toLowerCase() + " supplier");
        return supplier;
    }

    private Departure departure(Integer id) {
        return departures.findById(id).orElseThrow(() -> missing("Departure not found"));
    }

    private void checkTripDates(Departure departure, LocalDate start, LocalDate end) {
        checkDates(start, end);
        if (start.isBefore(departure.getStartDate())) badRequest("Service date is before the departure");
        if (end.isAfter(departure.getEndDate())) badRequest("Service date is after the trip ends");
    }

    private void checkContract(Supplier supplier, LocalDate start, LocalDate end) {
        if (start.isBefore(supplier.getContractStart()) || end.isAfter(supplier.getContractEnd())) {
            badRequest("These dates are outside the supplier contract");
        }
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) badRequest("Check the start and end dates");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) badRequest(field + " is required");
    }

    private void requireType(String value, String... allowed) {
        for (String option : allowed) if (option.equals(value)) return;
        badRequest("Choose a valid type");
    }

    private boolean positiveOrZero(BigDecimal value) { return value != null && value.signum() >= 0; }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
