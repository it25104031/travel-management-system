package com.travel;

import com.travel.module2_visa.domain.IndiaVisaApplicationEntity;
import com.travel.module2_visa.domain.SriLankaVisaApplicationEntity;
import com.travel.module2_visa.domain.CountryEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.domain.VisaTypeEntity;
import com.travel.module2_visa.service.VisaApplicationService;
import com.travel.module2_visa.service.VisaRuleService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private TestData() { }

    public static byte[] png(int width, int height) {
        try {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static MockMultipartFile photo(String param) {
        return new MockMultipartFile(param, "photo.png", "image/png", png(400, 400));
    }

    public static MockMultipartFile pdf(String param, String name) {
        return new MockMultipartFile(param, name, "application/pdf", "%PDF-1.4 test".getBytes());
    }

    public static VisaTypeEntity touristType(VisaRuleService rules) {
        CountryEntity india = rules.country("IN").orElseThrow();
        return rules.activeVisaTypes(india).stream().filter(t -> t.getName().equals("eTOURIST VISA")).findFirst().orElseThrow();
    }

    public static VisaApplicationEntity newApplication(Long visaTypeId) {
        int n = SEQ.incrementAndGet();
        IndiaVisaApplicationEntity app = new IndiaVisaApplicationEntity();
        app.setVisaTypeId(visaTypeId);
        app.setNationality("Sri Lankan");
        app.setGivenNames("Test" + n);
        app.setSurname("Traveller");
        app.setEmailId("traveller" + n + "@example.com");
        app.setPassportNumber("N" + n + "X");
        app.setPassportDateOfExpiry(LocalDate.now().plusYears(5));
        app.setExpectedDateOfArrival(LocalDate.now().plusMonths(2));
        app.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return app;
    }

    public static SriLankaVisaApplicationEntity newSriLankaApplication(Long visaTypeId) {
        int n = SEQ.incrementAndGet();
        SriLankaVisaApplicationEntity app = new SriLankaVisaApplicationEntity();
        app.setVisaTypeId(visaTypeId);
        app.setNationality("Indian");
        app.setGivenNames("Test" + n);
        app.setSurname("Visitor");
        app.setEmailId("visitor" + n + "@example.com");
        app.setContactNumber("+9170000" + n);
        app.setPassportNumber("L" + n + "K");
        app.setPassportDateOfExpiry(LocalDate.now().plusYears(5));
        app.setIntendedArrivalDate(LocalDate.now().plusMonths(2));
        app.setPurposeOfVisit("TOURISM");
        return app;
    }

    public static VisaTypeEntity sriLankaTouristType(VisaRuleService rules) {
        CountryEntity lk = rules.country("LK").orElseThrow();
        return rules.activeVisaTypes(lk).stream().filter(t -> t.getName().equals("Tourist ETA")).findFirst().orElseThrow();
    }

    public static Map<String, MultipartFile> requiredFiles() {
        Map<String, MultipartFile> files = new HashMap<>();
        files.put("doc_PHOTO", photo("doc_PHOTO"));
        files.put("doc_PASSPORT_BIO", pdf("doc_PASSPORT_BIO", "passport.pdf"));
        return files;
    }

    public static VisaApplicationService.SubmissionResult submit(VisaApplicationService service, VisaRuleService rules) {
        return service.submit(newApplication(touristType(rules).getId()), requiredFiles());
    }
}
