package com.travel.module2_visa.service;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.IncidentRepository;
import com.travel.module2_visa.repository.TourDepartureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** On-tour emergencies, including the automatic lost-passport workflow. */
@Service
public class IncidentService {

    static final String LOST_PASSPORT_CHECKLIST = String.join("\n",
            "Obtain a police report for the lost passport",
            "Contact the embassy / high commission for an Emergency Travel Document or replacement passport",
            "Take new passport photographs",
            "Upload the new passport bio page to the applicant portal",
            "Update the visa record with the new passport details (re-apply if required)",
            "Inform the airline of the new travel document number");

    static final String LOST_DOCUMENT_CHECKLIST = String.join("\n",
            "Identify which document was lost",
            "Obtain a police report if required",
            "Request a replacement from the issuing authority",
            "Upload the replacement document to the applicant portal");

    private final IncidentRepository incidentRepository;
    private final TourDepartureRepository departureRepository;
    private final VisaApplicationService applicationService;
    private final AdminUserRepository adminUserRepository;

    public IncidentService(IncidentRepository incidentRepository, TourDepartureRepository departureRepository,
                           VisaApplicationService applicationService, AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
        this.incidentRepository = incidentRepository;
        this.departureRepository = departureRepository;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> all() { return withTravellerNames(incidentRepository.findAllByOrderByCreatedAtDesc()); }

    @Transactional(readOnly = true)
    public List<IncidentEntity> forDeparture(Long departureId) {
        return withTravellerNames(incidentRepository.findByDepartureIdOrderByCreatedAtDesc(departureId));
    }

    // tour_incidents stores the passport number; show the traveller's name next to it
    private List<IncidentEntity> withTravellerNames(List<IncidentEntity> incidents) {
        for (IncidentEntity incident : incidents) {
            if (incident.getAffectedPassportNumber() == null) continue;
            incident.getDeparture().getTravellers().stream()
                    .filter(t -> incident.getAffectedPassportNumber().equalsIgnoreCase(t.getPassportNumber()))
                    .findFirst()
                    .ifPresent(t -> incident.setAffectedTravellerName(t.getFullName()));
        }
        return incidents;
    }

    @Transactional
    public IncidentEntity log(Long departureId, Long applicationId, IncidentType type, IncidentSeverity severity,
                              String description, String assignedTo, String actor) {
        if (type == null || severity == null) throw new ValidationException("Incident type and severity are required.");
        if (description == null || description.isBlank()) throw new ValidationException("Please describe the incident.");
        if ((type == IncidentType.LOST_PASSPORT || type == IncidentType.LOST_DOCUMENT) && applicationId == null) {
            throw new ValidationException("Select the traveller whose document was lost.");
        }

        IncidentEntity incident = new IncidentEntity();
        VisaApplicationEntity app = applicationId == null ? null : applicationService.get(applicationId);
        TourDepartureEntity departure = departureId != null
                ? departureRepository.findById(departureId).orElse(null)
                : app != null ? app.getDeparture() : null;
        if (departure == null) throw new ValidationException("Choose the departure this incident belongs to.");
        AdminUserEntity reporter = adminUserRepository.findByUsername(actor);
        if (reporter == null) throw new ValidationException("Only staff accounts can log incidents.");
        incident.setAffectedPassportNumber(app == null ? null : app.getPassportNumber());
        incident.setDeparture(departure);
        incident.setReportedByStaff(reporter);
        incident.setType(type.name());
        incident.setSeverity(severity.name());
        incident.setDescription(description.trim());
        incident.setAssignedTo(assignedTo == null || assignedTo.isBlank() ? actor : assignedTo);
        incident.setStatus("OPEN");

        if (type == IncidentType.LOST_PASSPORT) {
            incident.setReplacementChecklist(LOST_PASSPORT_CHECKLIST);
            invalidatePassport(app, departure);
        } else if (type == IncidentType.LOST_DOCUMENT) {
            incident.setReplacementChecklist(LOST_DOCUMENT_CHECKLIST);
        }
        return incidentRepository.save(incident);
    }

    /** Invalidates every current passport file, flags the traveller and re-locks the group's booking gate. */
    private void invalidatePassport(VisaApplicationEntity app, TourDepartureEntity departure) {
        app.setPassportInvalidated(true);
        for (ApplicationDocumentEntity doc : app.getCurrentDocuments()) {
            if (VisaApplicationService.PASSPORT_DOC_CODE.equals(doc.getDocCode())) {
                doc.setStatus(DocumentStatus.INVALIDATED.name());
                doc.setRejectionReason("Passport reported lost");
            }
        }
        if (departure != null && departure.getStatusEnum() == DepartureStatus.READY) {
            departure.setStatus(DepartureStatus.PENDING.name());
            departure.setReadyAt(null);
            departure.setReadyBy(null);
        }
        applicationService.notifyCustomer(app, "Lost passport: replacement steps",
                "We have recorded that your passport was lost. Your previous passport record has been invalidated.\n\n"
                        + "Please complete these steps:\n- " + LOST_PASSPORT_CHECKLIST.replace("\n", "\n- "));
    }

    @Transactional
    public void resolve(Long incidentId, String resolution) {
        if (resolution == null || resolution.isBlank()) throw new ValidationException("Describe how the incident was resolved.");
        IncidentEntity incident = incidentRepository.findById(incidentId).orElseThrow(() -> new ValidationException("Unknown incident."));
        incident.setResolution(resolution.trim());
        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
    }
}
