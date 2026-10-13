package com.travel.module2_visa.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry of destination-specific application forms. Adding a destination means adding an entry here,
 * an application entity + document entity for its tables, and its template; submission, review, documents,
 * portal, readiness and manifests are shared.
 */
public enum CountryForm {

    INDIA("india", "IN", "module2_visa/visa-apply-india", IndiaVisaApplicationEntity::new),
    SRI_LANKA("sri-lanka", "LK", "module2_visa/visa-apply-sri-lanka", SriLankaVisaApplicationEntity::new);

    private final String slug;
    private final String countryCode;
    private final String template;
    private final Supplier<VisaApplicationEntity> factory;

    CountryForm(String slug, String countryCode, String template, Supplier<VisaApplicationEntity> factory) {
        this.factory = factory;
        this.slug = slug;
        this.countryCode = countryCode;
        this.template = template;
    }

    public String getSlug() { return slug; }
    public String getCountryCode() { return countryCode; }
    public String getTemplate() { return template; }
    public String getRoute() { return "/visas/apply/" + slug; }

    /** A new, empty application of this destination's entity type. */
    public VisaApplicationEntity newApplication() { return factory.get(); }

    public static Optional<CountryForm> bySlug(String slug) {
        return Arrays.stream(values()).filter(f -> f.slug.equalsIgnoreCase(slug)).findFirst();
    }

    public static Optional<CountryForm> byCountryCode(String code) {
        return Arrays.stream(values()).filter(f -> f.countryCode.equalsIgnoreCase(code)).findFirst();
    }
}
