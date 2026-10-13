package com.travel.module2_visa.domain;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

/**
 * One mandatory (or optional) upload slot for a visa type. A null nationality means the
 * rule applies to every applicant; otherwise only to applicants of that nationality.
 */
@Entity
@Table(name = "visa_requirement_rules")
public class RequiredDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "visa_type_id")
    private VisaTypeEntity visaType;

    private String nationality;

    // Upload slot key (PHOTO, PASSPORT_BIO, ...); added to visa_requirement_rules, filled from document_name when missing
    private String docCode;

    @Column(name = "document_name", nullable = false)
    private String label;

    @Column(name = "accepted_formats")
    private String allowedFormats = "JPG,JPEG,PNG,PDF";

    @Column(name = "max_size_mb")
    private Integer maxSizeMb = 5;

    private Integer minWidthPx;

    private Integer minHeightPx;

    @Column(name = "is_mandatory")
    private Boolean mandatory = true;

    public List<String> getAllowedFormatList() {
        if (allowedFormats == null || allowedFormats.isBlank()) return List.of();
        return Arrays.stream(allowedFormats.toLowerCase().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Value for the HTML accept attribute, e.g. ".jpg,.png,.pdf". */
    public String getAcceptAttribute() {
        return String.join(",", getAllowedFormatList().stream().map(f -> "." + f).toList());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VisaTypeEntity getVisaType() { return visaType; }
    public void setVisaType(VisaTypeEntity visaType) { this.visaType = visaType; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getDocCode() { return docCode; }
    public void setDocCode(String docCode) { this.docCode = docCode; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getAllowedFormats() { return allowedFormats; }
    public void setAllowedFormats(String allowedFormats) { this.allowedFormats = allowedFormats; }

    public Integer getMaxSizeMb() { return maxSizeMb; }
    public void setMaxSizeMb(Integer maxSizeMb) { this.maxSizeMb = maxSizeMb; }

    public Integer getMinWidthPx() { return minWidthPx; }
    public void setMinWidthPx(Integer minWidthPx) { this.minWidthPx = minWidthPx; }

    public Integer getMinHeightPx() { return minHeightPx; }
    public void setMinHeightPx(Integer minHeightPx) { this.minHeightPx = minHeightPx; }

    public Boolean getMandatory() { return mandatory; }
    public void setMandatory(Boolean mandatory) { this.mandatory = mandatory; }
}
