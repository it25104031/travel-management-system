package com.travel.module2_visa.service;

import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Operator work queue: filtering, prioritising by travel date and flagging overdue applications. */
@Service
public class VisaQueueService {

    public record QueueRow(VisaApplicationEntity app, String passportDisplay, boolean overdue, LocalDate dueDate,
                           LocalDate travelDate, PassportValidityService.PassportCheck passport) { }

    private final VisaApplicationService applicationService;
    private final VisaRuleService ruleService;
    private final PassportValidityService passportService;

    public VisaQueueService(VisaApplicationService applicationService, VisaRuleService ruleService,
                            PassportValidityService passportService) {
        this.applicationService = applicationService;
        this.ruleService = ruleService;
        this.passportService = passportService;
    }

    /**
     * @param filter all | mine | unassigned | overdue | open | closed
     * @param maskPassports true for operators (ROLE_USER) so full numbers are not shown in lists
     */
    @Transactional(readOnly = true)
    public List<QueueRow> queue(String filter, String status, String currentUser, boolean maskPassports) {
        String f = filter == null ? "all" : filter;
        return applicationService.findAll().stream()
                .map(app -> toRow(app, maskPassports))
                .filter(row -> switch (f) {
                    case "mine" -> currentUser.equals(row.app().getAssignedOperator());
                    case "unassigned" -> row.app().getAssignedOperator() == null;
                    case "overdue" -> row.overdue();
                    case "open" -> !row.app().getStatusEnum().isClosed();
                    case "closed" -> row.app().getStatusEnum().isClosed();
                    default -> true;
                })
                .filter(row -> status == null || status.isBlank() || row.app().getStatusEnum().name().equals(status))
                // Overdue first, then soonest travel date, then oldest application
                .sorted(Comparator.comparing(QueueRow::overdue).reversed()
                        .thenComparing(QueueRow::travelDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.app().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public QueueRow toRow(VisaApplicationEntity app, boolean maskPassports) {
        LocalDateTime created = app.getCreatedAt() == null ? LocalDateTime.now() : app.getCreatedAt();
        LocalDate due = created.toLocalDate().plusDays(ruleService.processingDays(app.getVisaTypeId()));
        ApplicationStatus status = app.getStatusEnum();
        boolean overdue = !status.isClosed() && LocalDate.now().isAfter(due);
        LocalDate travel = app.getDeparture() != null ? app.getDeparture().getDepartureDate() : app.getExpectedDateOfArrival();
        String passport = maskPassports ? MaskingUtil.mask(app.getPassportNumber()) : app.getPassportNumber();
        return new QueueRow(app, passport, overdue, due, travel, passportService.check(app));
    }
}
