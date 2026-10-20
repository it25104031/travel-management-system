package com.travel.module2_visa.service;

import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.CountryEntity;
import com.travel.module2_visa.domain.RequiredDocumentEntity;
import com.travel.module2_visa.domain.VisaTypeEntity;
import com.travel.module2_visa.repository.CountryRepository;
import com.travel.module2_visa.repository.RequiredDocumentRepository;
import com.travel.module2_visa.repository.VisaTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class VisaRuleService {

    public static final int DEFAULT_PROCESSING_DAYS = 5;

    private final CountryRepository countryRepository;
    private final VisaTypeRepository visaTypeRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;

    public VisaRuleService(CountryRepository countryRepository, VisaTypeRepository visaTypeRepository,
                           RequiredDocumentRepository requiredDocumentRepository) {
        this.countryRepository = countryRepository;
        this.visaTypeRepository = visaTypeRepository;
        this.requiredDocumentRepository = requiredDocumentRepository;
    }

    @Transactional(readOnly = true)
    public List<CountryEntity> allCountries() { return countryRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<CountryEntity> activeCountries() { return countryRepository.findByIsActiveTrue(); }

    @Transactional(readOnly = true)
    public Optional<CountryEntity> country(String code) { return countryRepository.findByCountryCode(code); }

    @Transactional
    public CountryEntity addCountry(String name, String code, String formRoute) {
        if (countryRepository.findByCountryCode(code.trim().toUpperCase()).isPresent()) {
            throw new ValidationException("Country code " + code + " already exists.");
        }
        CountryEntity country = new CountryEntity();
        country.setCountryName(name.trim());
        country.setCountryCode(code.trim().toUpperCase());
        country.setFormRoute(formRoute.trim());
        country.setIsActive(true);
        return countryRepository.save(country);
    }

    @Transactional
    public void toggleCountry(Long id) {
        CountryEntity country = countryRepository.findById(id).orElseThrow(() -> new ValidationException("Unknown country."));
        country.setIsActive(!Boolean.TRUE.equals(country.getIsActive()));
    }

    @Transactional(readOnly = true)
    public List<VisaTypeEntity> allVisaTypes() { return visaTypeRepository.findAllByOrderByCountryCountryNameAscNameAsc(); }

    @Transactional(readOnly = true)
    public List<VisaTypeEntity> activeVisaTypes(CountryEntity country) {
        return visaTypeRepository.findByCountryAndActiveTrueOrderByNameAsc(country);
    }

    @Transactional(readOnly = true)
    public Optional<VisaTypeEntity> visaType(Long id) {
        return id == null ? Optional.empty() : visaTypeRepository.findById(id);
    }

    @Transactional
    public VisaTypeEntity addVisaType(Long countryId, String name, String category, BigDecimal fee, String currency, Integer processingDays) {
        CountryEntity country = countryRepository.findById(countryId).orElseThrow(() -> new ValidationException("Unknown country."));
        if (name == null || name.isBlank()) throw new ValidationException("Visa type name is required.");
        VisaTypeEntity type = new VisaTypeEntity();
        type.setCountry(country);
        type.setName(name.trim());
        type.setCategory(category);
        type.setFee(fee);
        type.setCurrency(currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase());
        type.setProcessingDays(processingDays == null ? DEFAULT_PROCESSING_DAYS : processingDays);
        type.setActive(true);
        return visaTypeRepository.save(type);
    }

    @Transactional
    public void updateVisaType(Long id, BigDecimal fee, Integer processingDays, boolean active) {
        VisaTypeEntity type = visaTypeRepository.findById(id).orElseThrow(() -> new ValidationException("Unknown visa type."));
        type.setFee(fee);
        type.setProcessingDays(processingDays == null ? DEFAULT_PROCESSING_DAYS : processingDays);
        type.setActive(active);
    }

    @Transactional(readOnly = true)
    public int processingDays(Long visaTypeId) {
        return visaType(visaTypeId).map(VisaTypeEntity::getProcessingDays).filter(d -> d != null && d > 0).orElse(DEFAULT_PROCESSING_DAYS);
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentEntity> rulesForType(VisaTypeEntity type) {
        return requiredDocumentRepository.findByVisaTypeOrderByIdAsc(type);
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentEntity> allRequiredDocuments() { return requiredDocumentRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<RequiredDocumentEntity> requiredDocuments(Long visaTypeId, String nationality) {
        Optional<VisaTypeEntity> type = visaType(visaTypeId);
        if (type.isEmpty()) return List.of();
        return requiredDocumentRepository.findByVisaTypeOrderByIdAsc(type.get()).stream()
                .filter(r -> r.getNationality() == null || r.getNationality().isBlank()
                        || (nationality != null && r.getNationality().trim().equalsIgnoreCase(nationality.trim())))
                .toList();
    }

    @Transactional
    public void addRequiredDocument(Long visaTypeId, String docCode, String label, String nationality, String allowedFormats,
                                    Integer maxSizeMb, Integer minWidthPx, Integer minHeightPx, boolean mandatory) {
        VisaTypeEntity type = visaTypeRepository.findById(visaTypeId).orElseThrow(() -> new ValidationException("Unknown visa type."));
        if (docCode == null || docCode.isBlank() || label == null || label.isBlank()) {
            throw new ValidationException("Document code and label are required.");
        }
        RequiredDocumentEntity rule = new RequiredDocumentEntity();
        rule.setVisaType(type);
        rule.setDocCode(docCode.trim().toUpperCase().replace(' ', '_'));
        rule.setLabel(label.trim());
        rule.setNationality(nationality == null || nationality.isBlank() ? null : nationality.trim());
        rule.setAllowedFormats(allowedFormats == null || allowedFormats.isBlank() ? "jpg,jpeg,png,pdf" : allowedFormats.toLowerCase().replace(" ", ""));
        rule.setMaxSizeMb(maxSizeMb == null ? 5 : maxSizeMb);
        rule.setMinWidthPx(minWidthPx);
        rule.setMinHeightPx(minHeightPx);
        rule.setMandatory(mandatory);
        requiredDocumentRepository.save(rule);
    }

    @Transactional
    public void deleteRequiredDocument(Long ruleId) {
        requiredDocumentRepository.deleteById(ruleId);
    }

    @Transactional
    public CountryEntity save(CountryEntity country) { return countryRepository.save(country); }

    @Transactional
    public VisaTypeEntity save(VisaTypeEntity type) { return visaTypeRepository.save(type); }

    @Transactional
    public RequiredDocumentEntity save(RequiredDocumentEntity rule) { return requiredDocumentRepository.save(rule); }

    @Transactional(readOnly = true)
    public boolean hasVisaTypes(CountryEntity country) { return visaTypeRepository.existsByCountry(country); }
}
