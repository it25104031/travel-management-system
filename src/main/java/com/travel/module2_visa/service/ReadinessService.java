package com.travel.module2_visa.service;

import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.TourDepartureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes traveller and departure-group readiness and enforces the booking gate:
 * a departure can only be marked READY when every requirement of every traveller is met.
 */
@Service
public class ReadinessService {

    public record CheckItem(String name, boolean ok, String detail) { }

    public record TravellerReadiness(VisaApplicationEntity application, List<CheckItem> items) {
        public long getSatisfied() { return items.stream().filter(CheckItem::ok).count(); }
        public int getTotal() { return items.size(); }
        public boolean isCleared() { return getSatisfied() == getTotal(); }
        public List<CheckItem> getBlockers() { return items.stream().filter(i -> !i.ok()).toList(); }
    }

    public record DepartureReadiness(TourDepartureEntity departure, List<TravellerReadiness> travellers,
                                     long satisfied, long total, BigDecimal percent) {
        public boolean isComplete() { return total > 0 && satisfied == total; }
        public long getClearedTravellers() { return travellers.stream().filter(TravellerReadiness::isCleared).count(); }
    }

    private final VisaApplicationService applicationService;
    private final PassportValidityService passportService;
    private final ComplianceService complianceService;
    private final TourDepartureRepository departureRepository;

    public ReadinessService(VisaApplicationService applicationService, PassportValidityService passportService,
                            ComplianceService complianceService, TourDepartureRepository departureRepository) {
        this.applicationService = applicationService;
        this.passportService = passportService;
        this.complianceService = complianceService;
        this.departureRepository = departureRepository;
    }

    @Transactional(readOnly = true)
    public TravellerReadiness forTraveller(VisaApplicationEntity app) {
        List<CheckItem> items = new ArrayList<>();

        ApplicationStatus status = app.getStatusEnum();
        items.add(new CheckItem("Visa approved", status == ApplicationStatus.APPROVED, status.getLabel()));

        List<String> docBlockers = applicationService.documentBlockers(app);
        items.add(new CheckItem("Documents verified", docBlockers.isEmpty(),
                docBlockers.isEmpty() ? "All mandatory documents verified" : String.join("; ", docBlockers)));

        PassportValidityService.PassportCheck passport = passportService.check(app);
        items.add(new CheckItem("Passport 6-month rule", passport.isCompliant(), passport.getLabel()));

        ComplianceRecordEntity record = complianceService.record(app);
        boolean insuranceOk = false;
        String insuranceDetail;
        if (app.getDeparture() == null) {
            insuranceDetail = "Not on a departure yet";
        } else if (record == null || !record.isInsuranceSubmitted()) {
            insuranceDetail = "Not submitted";
        } else {
            boolean covers = record.insuranceCovers(app.getDeparture().getDepartureDate(), PassportValidityService.travelEnd(app));
            boolean verified = Boolean.TRUE.equals(record.getInsuranceVerified());
            insuranceOk = verified && covers;
            insuranceDetail = !covers ? "Cover does not span the trip" : verified ? "Verified" : "Awaiting verification";
        }
        items.add(new CheckItem("Travel insurance", insuranceOk, insuranceDetail));

        boolean healthOk = record != null && record.isHealthVerified();
        items.add(new CheckItem("Health declaration & emergency contact", healthOk,
                record == null || !record.isHealthSubmitted() ? "Not submitted" : healthOk ? "Verified" : "Awaiting verification"));

        return new TravellerReadiness(app, items);
    }

    @Transactional(readOnly = true)
    public DepartureReadiness forDeparture(Long departureId) {
        TourDepartureEntity departure = departure(departureId);
        List<TravellerReadiness> travellers = departure.getTravellers().stream().map(this::forTraveller).toList();
        long satisfied = travellers.stream().mapToLong(TravellerReadiness::getSatisfied).sum();
        long total = travellers.stream().mapToLong(TravellerReadiness::getTotal).sum();
        return new DepartureReadiness(departure, travellers, satisfied, total, percent(satisfied, total));
    }

    /** Recomputes readiness and stores it in tour_departures.readiness_percentage. */
    @Transactional
    public DepartureReadiness refresh(Long departureId) {
        DepartureReadiness readiness = forDeparture(departureId);
        if (readiness.departure().getReadinessPercentage() == null
                || readiness.departure().getReadinessPercentage().compareTo(readiness.percent()) != 0) {
            readiness.departure().setReadinessPercentage(readiness.percent());
            departureRepository.save(readiness.departure());
        }
        return readiness;
    }

    /** Exact percentage with two decimals; 100.00 only when every item is satisfied. */
    public static BigDecimal percent(long satisfied, long total) {
        if (total == 0) return BigDecimal.ZERO.setScale(2);
        if (satisfied == total) return new BigDecimal("100.00");
        // Round down so an incomplete group can never display as 100%
        return BigDecimal.valueOf(satisfied).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.DOWN);
    }

    /** The booking gate. */
    @Transactional
    public void markReady(Long departureId, String actor) {
        DepartureReadiness readiness = refresh(departureId);
        TourDepartureEntity departure = readiness.departure();
        if (departure.getStatusEnum() != DepartureStatus.PENDING) {
            throw new ValidationException("Only PENDING departures can be marked ready.");
        }
        if (readiness.travellers().isEmpty()) {
            throw new ValidationException("Add travellers before clearing the departure.");
        }
        if (!readiness.isComplete()) {
            throw new ValidationException("Booking gate locked: readiness is " + readiness.percent()
                    + "%. All requirements must be verified (100%) before the departure can be marked READY.");
        }
        departure.setStatus(DepartureStatus.READY.name());
        departure.setReadyBy(actor);
        departure.setReadyAt(LocalDateTime.now());
    }

    /** Emails each traveller who is not yet cleared with the items still outstanding. Returns how many were sent. */
    @Transactional(readOnly = true)
    public int sendReminders(Long departureId) {
        DepartureReadiness readiness = forDeparture(departureId);
        int sent = 0;
        for (TravellerReadiness t : readiness.travellers()) {
            if (t.isCleared()) continue;
            StringBuilder body = new StringBuilder("Your departure " + readiness.departure().getCode() + " on "
                    + readiness.departure().getDepartureDate() + " cannot be cleared until these items are complete:\n");
            t.getBlockers().forEach(b -> body.append("- ").append(b.name()).append(": ").append(b.detail()).append('\n'));
            body.append("\nPlease sign in to the applicant portal to complete them.");
            applicationService.notifyCustomer(t.application(), "Action needed before your departure", body.toString());
            sent++;
        }
        return sent;
    }

    private TourDepartureEntity departure(Long id) {
        TourDepartureEntity departure = departureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown departure " + id));
        departure.getTravellers().size();
        return departure;
    }
}
