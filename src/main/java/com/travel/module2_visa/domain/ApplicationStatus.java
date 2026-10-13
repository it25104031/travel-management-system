package com.travel.module2_visa.domain;

/** Application lifecycle used by the report and activity diagram. */
public enum ApplicationStatus {
    PENDING("Pending Review"),
    SUBMITTED("Submitted to Government"),
    APPROVED("Visa Approved"),
    REJECTED("Visa Rejected");

    private final String label;

    ApplicationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isClosed() { return this == APPROVED || this == REJECTED; }

    /** Maps values written by the earlier version of the system. */
    public static ApplicationStatus fromLegacy(String value) {
        if (value == null) return PENDING;
        return switch (value) {
            case "COMPLETED" -> APPROVED;
            case "DRAFT" -> PENDING;
            default -> {
                try { yield valueOf(value); } catch (IllegalArgumentException e) { yield PENDING; }
            }
        };
    }
}
