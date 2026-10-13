package com.travel.module2_visa.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "india_visa_application_documents")
public class IndiaVisaApplicationDocumentEntity extends ApplicationDocumentEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "visa_application_id", nullable = false)
    private IndiaVisaApplicationEntity visaApplication;

    @Override
    public IndiaVisaApplicationEntity getVisaApplication() { return visaApplication; }
    public void setVisaApplication(IndiaVisaApplicationEntity visaApplication) { this.visaApplication = visaApplication; }
}
