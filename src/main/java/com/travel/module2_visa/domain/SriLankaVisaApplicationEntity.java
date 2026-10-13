package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Sri Lanka ETA application (table srilanka_visa_applications). Shared fields live in {@link VisaApplicationEntity}. */
@Entity
@Table(name = "srilanka_visa_applications")
public class SriLankaVisaApplicationEntity extends VisaApplicationEntity {

    private LocalDate intendedArrivalDate;
    private String purposeOfVisit; // TOURISM / BUSINESS / TRANSIT / VISITING FRIENDS & RELATIVES
    private String contactNumber;

    @OneToMany(mappedBy = "visaApplication", cascade = CascadeType.ALL)
    @OrderBy("uploadedAt ASC")
    private List<SriLankaVisaApplicationDocumentEntity> documents = new ArrayList<>();

    @Override
    public CountryForm getCountryForm() { return CountryForm.SRI_LANKA; }

    @Override
    public LocalDate getExpectedDateOfArrival() { return intendedArrivalDate; }

    @Override
    public List<SriLankaVisaApplicationDocumentEntity> getDocuments() { return documents; }

    @Override
    public ApplicationDocumentEntity newDocument() {
        SriLankaVisaApplicationDocumentEntity doc = new SriLankaVisaApplicationDocumentEntity();
        doc.setVisaApplication(this);
        documents.add(doc);
        return doc;
    }

    @Override
    public void copyTravelDetailsFrom(VisaApplicationEntity lead) {
        super.copyTravelDetailsFrom(lead);
        this.intendedArrivalDate = lead.getExpectedDateOfArrival();
        if (lead instanceof SriLankaVisaApplicationEntity sl) this.purposeOfVisit = sl.getPurposeOfVisit();
    }

    @Override
    public String getVisaService() {
        return "Sri Lanka ETA" + (purposeOfVisit == null ? "" : " (" + purposeOfVisit + ")");
    }

    public LocalDate getIntendedArrivalDate() { return intendedArrivalDate; }
    public void setIntendedArrivalDate(LocalDate intendedArrivalDate) { this.intendedArrivalDate = intendedArrivalDate; }

    public String getPurposeOfVisit() { return purposeOfVisit; }
    public void setPurposeOfVisit(String purposeOfVisit) { this.purposeOfVisit = purposeOfVisit; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}
