package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "india_visa_applications")
public class IndiaVisaApplicationEntity extends VisaApplicationEntity {

    private String passportType;
    private LocalDate expectedDateOfArrival;
    private String visaService;

    private String changedName;
    private String changedNameDetails;
    private String townCityOfBirth;
    private String countryOfBirth;
    private String citizenshipNationalIdNo;
    private String religion;
    private String visibleIdentificationMarks;
    private String educationalQualification;
    private String acquiredNationalityBy;
    private String livedInCountryTwoYears;

    private String passportPlaceOfIssue;
    private String otherPassportHeld;
    private String otherPassportCountry;
    private String otherPassportNo;
    private String otherPassportIssueDate;
    private String otherPassportPlace;
    private String otherPassportNationality;

    private String presentHouseNo;
    private String presentVillageTownCity;
    private String presentCountry;
    private String presentStateProvinceDistrict;
    private String presentPostalZipCode;
    private String presentPhoneNo;
    private String presentMobileNo;

    private String permanentHouseNo;
    private String permanentVillageTownCity;
    private String permanentStateProvinceDistrict;

    private String fatherName;
    private String fatherNationality;
    private String fatherPreviousNationality;
    private String fatherPlaceOfBirth;
    private String fatherCountryOfBirth;

    private String motherName;
    private String motherNationality;
    private String motherPreviousNationality;
    private String motherPlaceOfBirth;
    private String motherCountryOfBirth;

    private String maritalStatus;
    private String grandparentsPakistanOrigin;
    private String pakistanOriginDetails;

    @Column(name = "places_to_be_visited_line_1")
    private String placesToBeVisitedLine1;

    @Column(name = "places_to_be_visited_line_2")
    private String placesToBeVisitedLine2;

    private String bookedThroughTourOperator;
    private String expectedPortOfExit;

    private String visitedIndiaBefore;
    private String previousAddressInIndia;
    private String citiesPreviouslyVisited;
    private String lastIndianVisaNo;
    private String lastVisaType;
    private String lastVisaPlaceOfIssue;
    private String lastVisaDateOfIssue;
    private String permissionRefused;
    private String refusalDetails;

    @Column(name = "countries_visited_last_10_years")
    private String countriesVisitedLast10Years;

    private String visitedSaarcCountries;

    private String referenceNameInIndia;
    private String referenceAddressInIndia;
    private String referencePhoneInIndia;

    private String referenceNameInHomeCountry;
    private String referenceAddressInHomeCountry;
    private String referencePhoneInHomeCountry;

    private String arrestedOrProsecuted;
    private String refusedEntryOrDeported;
    private String engagedInTraffickingOrFraud;
    private String engagedInTerrorismOrCyberCrime;
    private String expressedGlorifyingViews;
    private String soughtAsylum;

    public String getPassportType() {
        return passportType;
    }

    public void setPassportType(String passportType) {
        this.passportType = passportType;
    }

    public LocalDate getExpectedDateOfArrival() {
        return expectedDateOfArrival;
    }

    public void setExpectedDateOfArrival(LocalDate expectedDateOfArrival) {
        this.expectedDateOfArrival = expectedDateOfArrival;
    }

    public String getVisaService() {
        return visaService;
    }

    public void setVisaService(String visaService) {
        this.visaService = visaService;
    }

    public String getChangedName() {
        return changedName;
    }

    public void setChangedName(String changedName) {
        this.changedName = changedName;
    }

    public String getChangedNameDetails() {
        return changedNameDetails;
    }

    public void setChangedNameDetails(String changedNameDetails) {
        this.changedNameDetails = changedNameDetails;
    }

    public String getTownCityOfBirth() {
        return townCityOfBirth;
    }

    public void setTownCityOfBirth(String townCityOfBirth) {
        this.townCityOfBirth = townCityOfBirth;
    }

    public String getCountryOfBirth() {
        return countryOfBirth;
    }

    public void setCountryOfBirth(String countryOfBirth) {
        this.countryOfBirth = countryOfBirth;
    }

    public String getCitizenshipNationalIdNo() {
        return citizenshipNationalIdNo;
    }

    public void setCitizenshipNationalIdNo(String citizenshipNationalIdNo) {
        this.citizenshipNationalIdNo = citizenshipNationalIdNo;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getVisibleIdentificationMarks() {
        return visibleIdentificationMarks;
    }

    public void setVisibleIdentificationMarks(String visibleIdentificationMarks) {
        this.visibleIdentificationMarks = visibleIdentificationMarks;
    }

    public String getEducationalQualification() {
        return educationalQualification;
    }

    public void setEducationalQualification(String educationalQualification) {
        this.educationalQualification = educationalQualification;
    }

    public String getAcquiredNationalityBy() {
        return acquiredNationalityBy;
    }

    public void setAcquiredNationalityBy(String acquiredNationalityBy) {
        this.acquiredNationalityBy = acquiredNationalityBy;
    }

    public String getLivedInCountryTwoYears() {
        return livedInCountryTwoYears;
    }

    public void setLivedInCountryTwoYears(String livedInCountryTwoYears) {
        this.livedInCountryTwoYears = livedInCountryTwoYears;
    }

    public String getPassportPlaceOfIssue() {
        return passportPlaceOfIssue;
    }

    public void setPassportPlaceOfIssue(String passportPlaceOfIssue) {
        this.passportPlaceOfIssue = passportPlaceOfIssue;
    }

    public String getOtherPassportHeld() {
        return otherPassportHeld;
    }

    public void setOtherPassportHeld(String otherPassportHeld) {
        this.otherPassportHeld = otherPassportHeld;
    }

    public String getOtherPassportCountry() {
        return otherPassportCountry;
    }

    public void setOtherPassportCountry(String otherPassportCountry) {
        this.otherPassportCountry = otherPassportCountry;
    }

    public String getOtherPassportNo() {
        return otherPassportNo;
    }

    public void setOtherPassportNo(String otherPassportNo) {
        this.otherPassportNo = otherPassportNo;
    }

    public String getOtherPassportIssueDate() {
        return otherPassportIssueDate;
    }

    public void setOtherPassportIssueDate(String otherPassportIssueDate) {
        this.otherPassportIssueDate = otherPassportIssueDate;
    }

    public String getOtherPassportPlace() {
        return otherPassportPlace;
    }

    public void setOtherPassportPlace(String otherPassportPlace) {
        this.otherPassportPlace = otherPassportPlace;
    }

    public String getOtherPassportNationality() {
        return otherPassportNationality;
    }

    public void setOtherPassportNationality(String otherPassportNationality) {
        this.otherPassportNationality = otherPassportNationality;
    }

    public String getPresentHouseNo() {
        return presentHouseNo;
    }

    public void setPresentHouseNo(String presentHouseNo) {
        this.presentHouseNo = presentHouseNo;
    }

    public String getPresentVillageTownCity() {
        return presentVillageTownCity;
    }

    public void setPresentVillageTownCity(String presentVillageTownCity) {
        this.presentVillageTownCity = presentVillageTownCity;
    }

    public String getPresentCountry() {
        return presentCountry;
    }

    public void setPresentCountry(String presentCountry) {
        this.presentCountry = presentCountry;
    }

    public String getPresentStateProvinceDistrict() {
        return presentStateProvinceDistrict;
    }

    public void setPresentStateProvinceDistrict(String presentStateProvinceDistrict) {
        this.presentStateProvinceDistrict = presentStateProvinceDistrict;
    }

    public String getPresentPostalZipCode() {
        return presentPostalZipCode;
    }

    public void setPresentPostalZipCode(String presentPostalZipCode) {
        this.presentPostalZipCode = presentPostalZipCode;
    }

    public String getPresentPhoneNo() {
        return presentPhoneNo;
    }

    public void setPresentPhoneNo(String presentPhoneNo) {
        this.presentPhoneNo = presentPhoneNo;
    }

    public String getPresentMobileNo() {
        return presentMobileNo;
    }

    public void setPresentMobileNo(String presentMobileNo) {
        this.presentMobileNo = presentMobileNo;
    }

    public String getPermanentHouseNo() {
        return permanentHouseNo;
    }

    public void setPermanentHouseNo(String permanentHouseNo) {
        this.permanentHouseNo = permanentHouseNo;
    }

    public String getPermanentVillageTownCity() {
        return permanentVillageTownCity;
    }

    public void setPermanentVillageTownCity(String permanentVillageTownCity) {
        this.permanentVillageTownCity = permanentVillageTownCity;
    }

    public String getPermanentStateProvinceDistrict() {
        return permanentStateProvinceDistrict;
    }

    public void setPermanentStateProvinceDistrict(String permanentStateProvinceDistrict) {
        this.permanentStateProvinceDistrict = permanentStateProvinceDistrict;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getFatherNationality() {
        return fatherNationality;
    }

    public void setFatherNationality(String fatherNationality) {
        this.fatherNationality = fatherNationality;
    }

    public String getFatherPreviousNationality() {
        return fatherPreviousNationality;
    }

    public void setFatherPreviousNationality(String fatherPreviousNationality) {
        this.fatherPreviousNationality = fatherPreviousNationality;
    }

    public String getFatherPlaceOfBirth() {
        return fatherPlaceOfBirth;
    }

    public void setFatherPlaceOfBirth(String fatherPlaceOfBirth) {
        this.fatherPlaceOfBirth = fatherPlaceOfBirth;
    }

    public String getFatherCountryOfBirth() {
        return fatherCountryOfBirth;
    }

    public void setFatherCountryOfBirth(String fatherCountryOfBirth) {
        this.fatherCountryOfBirth = fatherCountryOfBirth;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getMotherNationality() {
        return motherNationality;
    }

    public void setMotherNationality(String motherNationality) {
        this.motherNationality = motherNationality;
    }

    public String getMotherPreviousNationality() {
        return motherPreviousNationality;
    }

    public void setMotherPreviousNationality(String motherPreviousNationality) {
        this.motherPreviousNationality = motherPreviousNationality;
    }

    public String getMotherPlaceOfBirth() {
        return motherPlaceOfBirth;
    }

    public void setMotherPlaceOfBirth(String motherPlaceOfBirth) {
        this.motherPlaceOfBirth = motherPlaceOfBirth;
    }

    public String getMotherCountryOfBirth() {
        return motherCountryOfBirth;
    }

    public void setMotherCountryOfBirth(String motherCountryOfBirth) {
        this.motherCountryOfBirth = motherCountryOfBirth;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getGrandparentsPakistanOrigin() {
        return grandparentsPakistanOrigin;
    }

    public void setGrandparentsPakistanOrigin(String grandparentsPakistanOrigin) {
        this.grandparentsPakistanOrigin = grandparentsPakistanOrigin;
    }

    public String getPakistanOriginDetails() {
        return pakistanOriginDetails;
    }

    public void setPakistanOriginDetails(String pakistanOriginDetails) {
        this.pakistanOriginDetails = pakistanOriginDetails;
    }

    public String getPlacesToBeVisitedLine1() {
        return placesToBeVisitedLine1;
    }

    public void setPlacesToBeVisitedLine1(String placesToBeVisitedLine1) {
        this.placesToBeVisitedLine1 = placesToBeVisitedLine1;
    }

    public String getPlacesToBeVisitedLine2() {
        return placesToBeVisitedLine2;
    }

    public void setPlacesToBeVisitedLine2(String placesToBeVisitedLine2) {
        this.placesToBeVisitedLine2 = placesToBeVisitedLine2;
    }

    public String getBookedThroughTourOperator() {
        return bookedThroughTourOperator;
    }

    public void setBookedThroughTourOperator(String bookedThroughTourOperator) {
        this.bookedThroughTourOperator = bookedThroughTourOperator;
    }

    public String getExpectedPortOfExit() {
        return expectedPortOfExit;
    }

    public void setExpectedPortOfExit(String expectedPortOfExit) {
        this.expectedPortOfExit = expectedPortOfExit;
    }

    public String getVisitedIndiaBefore() {
        return visitedIndiaBefore;
    }

    public void setVisitedIndiaBefore(String visitedIndiaBefore) {
        this.visitedIndiaBefore = visitedIndiaBefore;
    }

    public String getPreviousAddressInIndia() {
        return previousAddressInIndia;
    }

    public void setPreviousAddressInIndia(String previousAddressInIndia) {
        this.previousAddressInIndia = previousAddressInIndia;
    }

    public String getCitiesPreviouslyVisited() {
        return citiesPreviouslyVisited;
    }

    public void setCitiesPreviouslyVisited(String citiesPreviouslyVisited) {
        this.citiesPreviouslyVisited = citiesPreviouslyVisited;
    }

    public String getLastIndianVisaNo() {
        return lastIndianVisaNo;
    }

    public void setLastIndianVisaNo(String lastIndianVisaNo) {
        this.lastIndianVisaNo = lastIndianVisaNo;
    }

    public String getLastVisaType() {
        return lastVisaType;
    }

    public void setLastVisaType(String lastVisaType) {
        this.lastVisaType = lastVisaType;
    }

    public String getLastVisaPlaceOfIssue() {
        return lastVisaPlaceOfIssue;
    }

    public void setLastVisaPlaceOfIssue(String lastVisaPlaceOfIssue) {
        this.lastVisaPlaceOfIssue = lastVisaPlaceOfIssue;
    }

    public String getLastVisaDateOfIssue() {
        return lastVisaDateOfIssue;
    }

    public void setLastVisaDateOfIssue(String lastVisaDateOfIssue) {
        this.lastVisaDateOfIssue = lastVisaDateOfIssue;
    }

    public String getPermissionRefused() {
        return permissionRefused;
    }

    public void setPermissionRefused(String permissionRefused) {
        this.permissionRefused = permissionRefused;
    }

    public String getRefusalDetails() {
        return refusalDetails;
    }

    public void setRefusalDetails(String refusalDetails) {
        this.refusalDetails = refusalDetails;
    }

    public String getCountriesVisitedLast10Years() {
        return countriesVisitedLast10Years;
    }

    public void setCountriesVisitedLast10Years(String countriesVisitedLast10Years) {
        this.countriesVisitedLast10Years = countriesVisitedLast10Years;
    }

    public String getVisitedSaarcCountries() {
        return visitedSaarcCountries;
    }

    public void setVisitedSaarcCountries(String visitedSaarcCountries) {
        this.visitedSaarcCountries = visitedSaarcCountries;
    }

    public String getReferenceNameInIndia() {
        return referenceNameInIndia;
    }

    public void setReferenceNameInIndia(String referenceNameInIndia) {
        this.referenceNameInIndia = referenceNameInIndia;
    }

    public String getReferenceAddressInIndia() {
        return referenceAddressInIndia;
    }

    public void setReferenceAddressInIndia(String referenceAddressInIndia) {
        this.referenceAddressInIndia = referenceAddressInIndia;
    }

    public String getReferencePhoneInIndia() {
        return referencePhoneInIndia;
    }

    public void setReferencePhoneInIndia(String referencePhoneInIndia) {
        this.referencePhoneInIndia = referencePhoneInIndia;
    }

    public String getReferenceNameInHomeCountry() {
        return referenceNameInHomeCountry;
    }

    public void setReferenceNameInHomeCountry(String referenceNameInHomeCountry) {
        this.referenceNameInHomeCountry = referenceNameInHomeCountry;
    }

    public String getReferenceAddressInHomeCountry() {
        return referenceAddressInHomeCountry;
    }

    public void setReferenceAddressInHomeCountry(String referenceAddressInHomeCountry) {
        this.referenceAddressInHomeCountry = referenceAddressInHomeCountry;
    }

    public String getReferencePhoneInHomeCountry() {
        return referencePhoneInHomeCountry;
    }

    public void setReferencePhoneInHomeCountry(String referencePhoneInHomeCountry) {
        this.referencePhoneInHomeCountry = referencePhoneInHomeCountry;
    }

    public String getArrestedOrProsecuted() {
        return arrestedOrProsecuted;
    }

    public void setArrestedOrProsecuted(String arrestedOrProsecuted) {
        this.arrestedOrProsecuted = arrestedOrProsecuted;
    }

    public String getRefusedEntryOrDeported() {
        return refusedEntryOrDeported;
    }

    public void setRefusedEntryOrDeported(String refusedEntryOrDeported) {
        this.refusedEntryOrDeported = refusedEntryOrDeported;
    }

    public String getEngagedInTraffickingOrFraud() {
        return engagedInTraffickingOrFraud;
    }

    public void setEngagedInTraffickingOrFraud(String engagedInTraffickingOrFraud) {
        this.engagedInTraffickingOrFraud = engagedInTraffickingOrFraud;
    }

    public String getEngagedInTerrorismOrCyberCrime() {
        return engagedInTerrorismOrCyberCrime;
    }

    public void setEngagedInTerrorismOrCyberCrime(String engagedInTerrorismOrCyberCrime) {
        this.engagedInTerrorismOrCyberCrime = engagedInTerrorismOrCyberCrime;
    }

    public String getExpressedGlorifyingViews() {
        return expressedGlorifyingViews;
    }

    public void setExpressedGlorifyingViews(String expressedGlorifyingViews) {
        this.expressedGlorifyingViews = expressedGlorifyingViews;
    }

    public String getSoughtAsylum() {
        return soughtAsylum;
    }

    public void setSoughtAsylum(String soughtAsylum) {
        this.soughtAsylum = soughtAsylum;
    }

    @OneToMany(mappedBy = "visaApplication", cascade = CascadeType.ALL)
    @OrderBy("uploadedAt ASC")
    private List<IndiaVisaApplicationDocumentEntity> documents = new ArrayList<>();

    @Override
    public void copyTravelDetailsFrom(VisaApplicationEntity lead) {
        super.copyTravelDetailsFrom(lead);
        this.expectedDateOfArrival = lead.getExpectedDateOfArrival();
        if (!(lead instanceof IndiaVisaApplicationEntity t)) return;
        this.presentHouseNo = t.presentHouseNo;
        this.presentVillageTownCity = t.presentVillageTownCity;
        this.presentStateProvinceDistrict = t.presentStateProvinceDistrict;
        this.presentCountry = t.presentCountry;
        this.presentPostalZipCode = t.presentPostalZipCode;
        this.presentPhoneNo = t.presentPhoneNo;
        this.permanentHouseNo = t.permanentHouseNo;
        this.permanentVillageTownCity = t.permanentVillageTownCity;
        this.permanentStateProvinceDistrict = t.permanentStateProvinceDistrict;
        this.expectedPortOfExit = t.expectedPortOfExit;
        this.placesToBeVisitedLine1 = t.placesToBeVisitedLine1;
        this.placesToBeVisitedLine2 = t.placesToBeVisitedLine2;
        this.bookedThroughTourOperator = t.bookedThroughTourOperator;
    }

    @Override
    public CountryForm getCountryForm() { return CountryForm.INDIA; }

    @Override
    public List<IndiaVisaApplicationDocumentEntity> getDocuments() { return documents; }

    @Override
    public ApplicationDocumentEntity newDocument() {
        IndiaVisaApplicationDocumentEntity doc = new IndiaVisaApplicationDocumentEntity();
        doc.setVisaApplication(this);
        documents.add(doc);
        return doc;
    }
}
