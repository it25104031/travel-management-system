package com.travel.module2_visa.service;

import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.ApplicationStatusHistoryRepository;
import com.travel.module2_visa.repository.IncidentRepository;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    public record Summary(Map<String, Long> byStatus, long total, long approved, long rejected,
                          BigDecimal approvalRate, BigDecimal avgProcessingDays, Map<String, Long> incidentsByType,
                          Map<String, Long> byVisaService, List<CountryStat> byCountry) { }

    public record CountryStat(String countryCode, long total, long approved, long rejected, BigDecimal approvalRate) { }

    private final VisaApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final IncidentRepository incidentRepository;

    public ReportService(VisaApplicationRepository applicationRepository,
                         ApplicationStatusHistoryRepository historyRepository,
                         IncidentRepository incidentRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        List<VisaApplicationEntity> apps = applicationRepository.findAll();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) byStatus.put(s.name(), 0L);
        apps.forEach(a -> byStatus.merge(a.getStatusEnum().name(), 1L, Long::sum));

        long approved = byStatus.get(ApplicationStatus.APPROVED.name());
        long rejected = byStatus.get(ApplicationStatus.REJECTED.name());
        BigDecimal rate = rate(approved, rejected);

        List<ApplicationStatusHistoryEntity> decisions = historyRepository.findByToStatusIn(
                List.of(ApplicationStatus.APPROVED.name(), ApplicationStatus.REJECTED.name()));
        Map<String, ApplicationStatusHistoryEntity> firstDecision = new HashMap<>();
        for (ApplicationStatusHistoryEntity h : decisions) {
            firstDecision.merge(h.getApplicationType() + ":" + h.getApplicationId(), h,
                    (a, b) -> a.getChangedAt().isBefore(b.getChangedAt()) ? a : b);
        }
        Map<String, VisaApplicationEntity> appsByKey = new HashMap<>();
        apps.forEach(a -> appsByKey.put(a.getApplicationType() + ":" + a.getId(), a));
        BigDecimal avgDays = BigDecimal.ZERO;
        List<Long> minutes = firstDecision.entrySet().stream()
                .filter(e -> appsByKey.containsKey(e.getKey()) && appsByKey.get(e.getKey()).getCreatedAt() != null)
                .map(e -> Duration.between(appsByKey.get(e.getKey()).getCreatedAt(), e.getValue().getChangedAt()).toMinutes())
                .toList();
        if (!minutes.isEmpty()) {
            long sum = minutes.stream().mapToLong(Long::longValue).sum();
            avgDays = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(minutes.size() * 60L * 24L), 1, RoundingMode.HALF_UP);
        }

        Map<String, Long> incidents = incidentRepository.findAll().stream()
                .collect(Collectors.groupingBy(IncidentEntity::getType, TreeMap::new, Collectors.counting()));
        Map<String, Long> byService = apps.stream()
                .collect(Collectors.groupingBy(ReportService::serviceLabel,
                        TreeMap::new, Collectors.counting()));

        List<CountryStat> byCountry = apps.stream()
                .collect(Collectors.groupingBy(VisaApplicationEntity::getCountryCode, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    long ok = e.getValue().stream().filter(a -> a.getStatusEnum() == ApplicationStatus.APPROVED).count();
                    long no = e.getValue().stream().filter(a -> a.getStatusEnum() == ApplicationStatus.REJECTED).count();
                    return new CountryStat(e.getKey(), e.getValue().size(), ok, no, rate(ok, no));
                })
                .toList();

        return new Summary(byStatus, apps.size(), approved, rejected, rate, avgDays, incidents, byService, byCountry);
    }

    private static BigDecimal rate(long approved, long rejected) {
        return approved + rejected == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(approved * 100).divide(BigDecimal.valueOf(approved + rejected), 1, RoundingMode.HALF_UP);
    }

    private static String serviceLabel(VisaApplicationEntity a) {
        String service = a instanceof IndiaVisaApplicationEntity india ? india.getVisaService()
                : a instanceof SriLankaVisaApplicationEntity sl ? sl.getPurposeOfVisit() : null;
        return (service == null ? "(not set)" : service) + " (" + a.getCountryCode() + ")";
    }
}
