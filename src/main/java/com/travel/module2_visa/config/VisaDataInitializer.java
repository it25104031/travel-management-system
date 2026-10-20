package com.travel.module2_visa.config;

import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.CountryRepository;
import com.travel.module2_visa.repository.ApplicationDocumentRepository;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import com.travel.module2_visa.service.VisaRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Configuration
public class VisaDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(VisaDataInitializer.class);

    @Bean
    public CommandLineRunner initVisaData(CountryRepository countryRepository,
                                          VisaRuleService ruleService,
                                          VisaApplicationRepository applicationRepository,
                                          ApplicationDocumentRepository documentRepository,
                                          TransactionTemplate tx,
                                          JdbcTemplate jdbc) {
        return args -> {

            alignIdGenerator(jdbc, "visa_application_seq", "india_visa_applications", "srilanka_visa_applications");
            alignIdGenerator(jdbc, "visa_document_seq", "india_visa_application_documents", "srilanka_visa_application_documents");
            tx.executeWithoutResult(status -> seed(countryRepository, ruleService, applicationRepository, documentRepository));
        };
    }

    private void seed(CountryRepository countryRepository, VisaRuleService ruleService,
                      VisaApplicationRepository applicationRepository, ApplicationDocumentRepository documentRepository) {
        {
            CountryEntity india = country(countryRepository, "India", CountryForm.INDIA);
            CountryEntity sriLanka = country(countryRepository, "Sri Lanka", CountryForm.SRI_LANKA);

            if (!ruleService.hasVisaTypes(india)) {
                VisaTypeEntity tourist = type(ruleService, india, "eTOURIST VISA", "TOURIST_EVISA", "25.00", 4);
                VisaTypeEntity business = type(ruleService, india, "eBUSINESS VISA", "BUSINESS", "80.00", 5);
                VisaTypeEntity medical = type(ruleService, india, "eMEDICAL VISA", "MEDICAL", "80.00", 5);
                type(ruleService, india, "eCONFERENCE VISA", "SPECIAL_PERMIT", "80.00", 5);
                VisaTypeEntity group = type(ruleService, india, "GROUP TOUR VISA", "GROUP", "25.00", 7);

                for (VisaTypeEntity t : new VisaTypeEntity[]{tourist, business, medical, group}) {
                    rule(ruleService, t, "PHOTO", "Passport-size photograph (JPG/PNG, min 350x350 px)", "jpg,jpeg,png", 1, 350, 350, true);
                    rule(ruleService, t, "PASSPORT_BIO", "Passport bio-data page (PDF/JPG)", "pdf,jpg,jpeg,png", 2, null, null, true);
                }
                rule(ruleService, business, "BUSINESS_CARD", "Business card or invitation letter", "pdf,jpg,jpeg,png", 2, null, null, true);
                rule(ruleService, medical, "HOSPITAL_LETTER", "Letter from the treating hospital in India", "pdf", 2, null, null, true);
                rule(ruleService, group, "TOUR_ITINERARY", "Confirmed tour itinerary", "pdf", 2, null, null, false);
            }

            if (!ruleService.hasVisaTypes(sriLanka)) {
                VisaTypeEntity tourist = type(ruleService, sriLanka, "Tourist ETA", "TOURIST_EVISA", "50.00", 2);
                VisaTypeEntity business = type(ruleService, sriLanka, "Business ETA", "BUSINESS", "55.00", 3);
                VisaTypeEntity transit = type(ruleService, sriLanka, "Transit ETA", "TRANSIT", "0.00", 1);
                for (VisaTypeEntity t : new VisaTypeEntity[]{tourist, business, transit}) {
                    rule(ruleService, t, "PASSPORT_BIO", "Passport bio-data page (PDF/JPG)", "pdf,jpg,jpeg,png", 2, null, null, true);
                }
                rule(ruleService, tourist, "RETURN_TICKET", "Return or onward ticket", "pdf,jpg,jpeg,png", 2, null, null, false);
                rule(ruleService, business, "BUSINESS_LETTER", "Invitation letter from the Sri Lankan company", "pdf", 2, null, null, true);
                rule(ruleService, transit, "ONWARD_TICKET", "Confirmed onward ticket", "pdf,jpg,jpeg,png", 2, null, null, true);
            }

            for (VisaApplicationEntity app : applicationRepository.findAll()) {
                String normalised = app.getStatusEnum().name();
                if (!normalised.equals(app.getStatus())) app.setStatus(normalised);
                if (app.getPassportInvalidated() == null) app.setPassportInvalidated(false);
            }

            for (RequiredDocumentEntity rule : ruleService.allRequiredDocuments()) {
                if (rule.getDocCode() == null || rule.getDocCode().isBlank()) {
                    rule.setDocCode(rule.getLabel().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", ""));
                }
            }
            for (ApplicationDocumentEntity doc : documentRepository.findAll()) {
                if (doc.getStatus() == null) doc.setStatus(DocumentStatus.UPLOADED.name());
                if (doc.getVersion() == null) doc.setVersion(1);
                if (doc.getDocCode() == null) doc.setDocCode("OTHER");
            }
        }
    }

    static void alignIdGenerator(JdbcTemplate jdbc, String generator, String... tables) {
        long max = 0;
        for (String table : tables) {
            Long tableMax = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
            if (tableMax != null) max = Math.max(max, tableMax);
        }
        Integer rows;
        try {

            rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + generator, Integer.class);
        } catch (DataAccessException notATable) {
            rows = null;
        }
        try {
            if (rows == null) {

                jdbc.execute("ALTER SEQUENCE " + generator + " RESTART WITH " + (max + 1));
            } else if (rows == 0) {
                jdbc.update("INSERT INTO " + generator + " (next_val) VALUES (?)", max + 1);
            } else {
                jdbc.update("UPDATE " + generator + " SET next_val = ? WHERE next_val <= ?", max + 1, max);
            }
        } catch (DataAccessException e) {
            log.warn("Could not align id generator {}: {}", generator, e.getMessage());
        }
    }

    private static CountryEntity country(CountryRepository repository, String name, CountryForm form) {
        CountryEntity country = repository.findByCountryCode(form.getCountryCode())
                .or(() -> repository.findByCountryNameIgnoreCase(name))
                .orElseGet(() -> {
            CountryEntity c = new CountryEntity();
            c.setCountryName(name);
            c.setIsActive(true);
            return c;
        });
        country.setCountryCode(form.getCountryCode());
        country.setFormRoute(form.getRoute());
        return repository.save(country);
    }

    private static VisaTypeEntity type(VisaRuleService rules, CountryEntity country, String name, String category,
                                       String fee, int days) {
        VisaTypeEntity t = new VisaTypeEntity();
        t.setCountry(country);
        t.setName(name);
        t.setCategory(category);
        t.setFee(new BigDecimal(fee));
        t.setCurrency("USD");
        t.setProcessingDays(days);
        t.setActive(true);
        return rules.save(t);
    }

    private static void rule(VisaRuleService rules, VisaTypeEntity type, String code, String label, String formats,
                             int maxMb, Integer minW, Integer minH, boolean mandatory) {
        RequiredDocumentEntity r = new RequiredDocumentEntity();
        r.setVisaType(type);
        r.setDocCode(code);
        r.setLabel(label);
        r.setAllowedFormats(formats);
        r.setMaxSizeMb(maxMb);
        r.setMinWidthPx(minW);
        r.setMinHeightPx(minH);
        r.setMandatory(mandatory);
        rules.save(r);
    }
}
