package com.travel.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Company details shown on the public website (header, footer, contact section, bank details for
 * transfers). Change them in application.yml under app.site - no code changes needed.
 */
@ConfigurationProperties(prefix = "app.site")
public record SiteInfo(
        String name,
        String tagline,
        String phone,
        String email,
        String address,
        String hours,
        String whatsapp,
        String facebook,
        String instagram,
        String youtube,
        String tiktok,
        String bankName,
        String bankAccountName,
        String bankAccountNumber,
        String bankBranch) {

    public SiteInfo {
        name = or(name, "Dahamde Pilgrims & Tours");
        tagline = or(tagline, "Pilgrimages, holidays and visas - planned end to end.");
        phone = or(phone, "077 811 4262");
        email = or(email, "dhanuskape@gmail.com");
        address = or(address, "59/10 \"Samadhi Garden\", Pannipitiya 10230, Sri Lanka");
        hours = blank(hours);
        whatsapp = or(whatsapp, "https://wa.me/94777138924");
        facebook = or(facebook, "https://www.facebook.com/p/Dahamde-Pilgrims-Tours-61591633416052/");
        instagram = blank(instagram);
        youtube = blank(youtube);
        tiktok = blank(tiktok);
        bankName = or(bankName, "[Bank name]");
        bankAccountName = or(bankAccountName, "Dahamde Pilgrims & Tours");
        bankAccountNumber = or(bankAccountNumber, "[Account number]");
        bankBranch = or(bankBranch, "[Branch]");
    }

    /** "Dahamde Pilgrims & Tours" -> "DP" for the logo mark. */
    public String getMonogram() {
        String[] parts = name.trim().split("\\s+");
        String m = parts[0].substring(0, 1) + (parts.length > 1 ? parts[1].substring(0, 1) : "");
        return m.toUpperCase();
    }

    public String getPhoneLink() {
        String d = phone.replaceAll("[^+0-9]", "");
        // Sri Lankan local numbers (077 ...) get the +94 country code so the link works from abroad too
        if (d.matches("0\\d{9}")) d = "+94" + d.substring(1);
        return "tel:" + d;
    }

    /** WhatsApp number for display, taken from the wa.me link (e.g. +94 77 713 8924). */
    public String getWhatsappNumber() {
        String d = whatsapp.replaceAll("\\D", "");
        if (d.length() == 11 && d.startsWith("94")) {
            return "+94 " + d.substring(2, 4) + " " + d.substring(4, 7) + " " + d.substring(7);
        }
        return d.isEmpty() ? "" : "+" + d;
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String or(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
