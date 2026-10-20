package com.travel.module2_visa.service;

import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.CountryEntity;
import com.travel.module2_visa.domain.DepartureStatus;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.domain.TourDepartureEntity;
import com.travel.module2_visa.repository.CountryRepository;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import com.travel.module2_visa.repository.TourDepartureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DepartureService {

    private final TourDepartureRepository departureRepository;
    private final VisaApplicationRepository applicationRepository;
    private final VisaApplicationService applicationService;
    private final TempCredentialService credentialService;
    private final CountryRepository countryRepository;

    public DepartureService(TourDepartureRepository departureRepository, VisaApplicationRepository applicationRepository,
                            VisaApplicationService applicationService, TempCredentialService credentialService,
                            CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
        this.departureRepository = departureRepository;
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.credentialService = credentialService;
    }

    @Transactional(readOnly = true)
    public List<TourDepartureEntity> all() {
        List<TourDepartureEntity> list = departureRepository.findAllByOrderByDepartureDateAsc();
        list.forEach(d -> d.getTravellers().size());
        return list;
    }

    @Transactional(readOnly = true)
    public TourDepartureEntity get(Long id) {
        TourDepartureEntity d = departureRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown departure " + id));
        d.getTravellers().size();
        return d;
    }

    @Transactional(readOnly = true)
    public List<VisaApplicationEntity> unassignedApplications() {
        return applicationRepository.findByDepartureIsNullOrderBySurnameAsc();
    }

    @Transactional
    public TourDepartureEntity create(String code, String tourName, String destinationCountry, LocalDate departureDate, LocalDate returnDate) {
        List<String> errors = new ArrayList<>();
        if (code == null || code.isBlank()) errors.add("Departure code is required.");
        else if (departureRepository.existsByCode(code.trim().toUpperCase())) errors.add("Departure code already exists.");
        if (tourName == null || tourName.isBlank()) errors.add("Tour name is required.");
        CountryEntity country = destinationCountry == null ? null
                : countryRepository.findByCountryNameIgnoreCase(destinationCountry.trim()).orElse(null);
        if (country == null) errors.add("Choose the destination country.");
        if (departureDate == null || returnDate == null) errors.add("Departure and return dates are required.");
        else if (returnDate.isBefore(departureDate)) errors.add("Return date must be on or after the departure date.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        TourDepartureEntity departure = new TourDepartureEntity();
        departure.setCode(code.trim().toUpperCase());
        departure.setTourName(tourName.trim());
        departure.setDestinationCountry(country.getCountryName());
        departure.setDepartureDate(departureDate);
        departure.setReturnDate(returnDate);
        departure.setStatus(DepartureStatus.PENDING.name());
        return departureRepository.save(departure);
    }

    @Transactional
    public void addTravellers(Long departureId, List<Long> applicationIds) {
        TourDepartureEntity departure = get(departureId);
        requireOpen(departure);
        if (applicationIds == null || applicationIds.isEmpty()) throw new ValidationException("Select at least one traveller.");
        String destinationCode = countryRepository.findByCountryNameIgnoreCase(departure.getDestinationCountry())
                .map(CountryEntity::getCountryCode).orElse(null);
        List<VisaApplicationEntity> apps = applicationIds.stream().map(applicationService::get).toList();
        List<String> errors = new ArrayList<>();
        for (VisaApplicationEntity app : apps) {
            if (destinationCode != null && !destinationCode.equalsIgnoreCase(app.getCountryCode())) {
                errors.add(app.getFullName() + " applied for a different destination than " + departure.getDestinationCountry() + ".");
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        for (VisaApplicationEntity app : apps) {
            app.setDeparture(departure);
            departure.getTravellers().add(app);
        }
    }

    @Transactional
    public void removeTraveller(Long departureId, Long applicationId) {
        TourDepartureEntity departure = get(departureId);
        requireOpen(departure);
        VisaApplicationEntity app = applicationService.get(applicationId);
        if (app.getDeparture() != null && app.getDeparture().getId().equals(departureId)) {
            app.setDeparture(null);
            departure.getTravellers().remove(app);
        }
    }

    @Transactional
    public int copySharedDetails(Long departureId, Long leadApplicationId) {
        TourDepartureEntity departure = get(departureId);
        VisaApplicationEntity lead = applicationService.get(leadApplicationId);
        if (lead.getDeparture() == null || !lead.getDeparture().getId().equals(departureId)) {
            throw new ValidationException("The lead traveller must belong to this departure.");
        }
        int updated = 0;
        for (VisaApplicationEntity t : departure.getTravellers()) {
            if (t.getId().equals(lead.getId())) continue;
            t.copyTravelDetailsFrom(lead);
            updated++;
        }
        return updated;
    }

    @Transactional
    public int batchChangeStatus(Long departureId, List<Long> applicationIds, ApplicationStatus target, String reason, String actor) {
        TourDepartureEntity departure = get(departureId);
        if (applicationIds == null || applicationIds.isEmpty()) throw new ValidationException("Select at least one traveller.");
        if (target == null) throw new ValidationException("Choose the new status.");

        List<VisaApplicationEntity> selected = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Long id : applicationIds) {
            VisaApplicationEntity app = applicationService.get(id);
            if (app.getDeparture() == null || !app.getDeparture().getId().equals(departure.getId())) {
                errors.add(app.getFullName() + ": not in this departure.");
                continue;
            }
            applicationService.transitionErrors(app, target, reason)
                    .forEach(e -> errors.add(app.getFullName() + ": " + e));
            selected.add(app);
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        for (VisaApplicationEntity app : selected) {
            applicationService.changeStatus(app.getId(), target, reason, actor);
        }
        return selected.size();
    }

    @Transactional
    public void markDeparted(Long departureId) {
        TourDepartureEntity departure = get(departureId);
        if (departure.getStatusEnum() != DepartureStatus.READY) {
            throw new ValidationException("Only a READY departure can be marked as departed.");
        }
        departure.setStatus(DepartureStatus.DEPARTED.name());
        departure.getTravellers().forEach(t -> credentialService.deactivateForApplication(t.getId(), "Departure closed"));
    }

    @Transactional
    public void reopen(Long departureId) {
        TourDepartureEntity departure = get(departureId);
        if (departure.getStatusEnum() == DepartureStatus.READY) {
            departure.setStatus(DepartureStatus.PENDING.name());
            departure.setReadyAt(null);
            departure.setReadyBy(null);
        }
    }

    private void requireOpen(TourDepartureEntity departure) {
        if (departure.getStatusEnum() != DepartureStatus.PENDING) {
            throw new ValidationException("The departure is " + departure.getStatus() + "; reopen it before changing travellers.");
        }
    }
}
