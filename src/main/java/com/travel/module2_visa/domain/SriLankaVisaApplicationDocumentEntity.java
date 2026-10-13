package com.travel.module2_visa.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "srilanka_visa_application_documents")
public class SriLankaVisaApplicationDocumentEntity extends ApplicationDocumentEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "visa_application_id", nullable = false)
    private SriLankaVisaApplicationEntity visaApplication;

    @Override
    public SriLankaVisaApplicationEntity getVisaApplication() { return visaApplication; }
    public void setVisaApplication(SriLankaVisaApplicationEntity visaApplication) { this.visaApplication = visaApplication; }
}
