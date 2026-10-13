package com.travel;

import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.IncidentSeverity;
import com.travel.module2_visa.domain.IncidentType;
import com.travel.module2_visa.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The test database is created from the team's schema (src/test/resources/team-schema.sql). These checks read the
 * rows back with plain SQL using the team's table and column names, so a mapping that drifted from the agreed
 * schema (for example writing to a new column instead of the existing one) fails here.
 */
@SpringBootTest
class TeamSchemaTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired VisaApplicationService applicationService;
    @Autowired VisaRuleService ruleService;
    @Autowired DepartureService departureService;
    @Autowired ComplianceService complianceService;
    @Autowired IncidentService incidentService;

    @Test
    void everyFeatureWritesIntoTheTeamTablesAndColumns() {
        var india = applicationService.submit(TestData.newApplication(TestData.touristType(ruleService).getId()),
                TestData.requiredFiles()).application();
        var lk = applicationService.submit(TestData.newSriLankaApplication(TestData.sriLankaTouristType(ruleService).getId()),
                Map.of("doc_PASSPORT_BIO", TestData.pdf("doc_PASSPORT_BIO", "p.pdf"))).application();
        assertNotEquals(india.getId(), lk.getId(), "application ids are shared across the destination tables");

        Map<String, Object> in = jdbc.queryForMap("SELECT given_names, passport_number, status, expected_date_of_arrival, visa_service "
                + "FROM india_visa_applications WHERE id = ?", india.getId());
        assertEquals(india.getPassportNumber(), in.get("PASSPORT_NUMBER"));
        assertEquals("PENDING", in.get("STATUS"));
        assertEquals("eTOURIST VISA", in.get("VISA_SERVICE"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM india_visa_application_documents WHERE visa_application_id = ? "
                + "AND verification_status = 'UPLOADED' AND version_number = 1 AND document_type IN ('PHOTO', 'PASSPORT_BIO')",
                Integer.class, india.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM srilanka_visa_applications WHERE id = ? AND purpose_of_visit = 'TOURISM'",
                Integer.class, lk.getId()));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM customer_accounts WHERE reference_number LIKE 'IN-%' "
                + "AND customer_email = ? AND is_active = TRUE", Integer.class, india.getEmailId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM visa_status_history WHERE application_type = 'SRI_LANKA' "
                + "AND application_id = ? AND new_status = 'PENDING' AND changed_by_user = 'applicant'", Integer.class, lk.getId()));

        applicationService.assign(india.getId(), "operator");
        applicationService.get(india.getId()).getCurrentDocuments().forEach(d -> applicationService.verifyDocument(d.getId(), "operator"));
        applicationService.recordGovReference(india.getId(), "IND-GOV-77");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM india_visa_application_documents WHERE visa_application_id = ? "
                + "AND verification_status = 'VERIFIED'", Integer.class, india.getId()));
        applicationService.changeStatus(india.getId(), ApplicationStatus.SUBMITTED, null, "operator");
        Map<String, Object> tracked = jdbc.queryForMap("SELECT assigned_operator_id, gov_submission_date, gov_portal_ref_no FROM india_visa_applications WHERE id = ?", india.getId());
        assertEquals(jdbc.queryForObject("SELECT id FROM admin_users WHERE username = 'operator'", Long.class),
                ((Number) tracked.get("ASSIGNED_OPERATOR_ID")).longValue());
        assertNotNull(tracked.get("GOV_SUBMISSION_DATE"));
        assertEquals("IND-GOV-77", tracked.get("GOV_PORTAL_REF_NO"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM visa_status_history WHERE application_type = 'INDIA' "
                + "AND application_id = ? AND previous_status = 'PENDING' AND new_status = 'SUBMITTED'", Integer.class, india.getId()));

        var departure = departureService.create("SCHEMA-" + india.getId(), "Schema Tour", "India",
                LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(7));
        departureService.addTravellers(departure.getId(), List.of(india.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tour_departures WHERE departure_code = ? "
                + "AND destination_country = 'India' AND booking_gate_status = 'PENDING'", Integer.class, departure.getCode()));
        assertEquals(departure.getId(), jdbc.queryForObject("SELECT departure_id FROM india_visa_applications WHERE id = ?", Long.class, india.getId()));

        complianceService.saveInsurance(india.getId(), "Ceylinco", "POL-9", departure.getDepartureDate(), departure.getReturnDate());
        complianceService.saveHealth(india.getId(), true, "Yellow fever", "Wheelchair at airports", "Parent", "+94770000000");
        Map<String, Object> record = jdbc.queryForMap("SELECT insurance_provider, insurance_policy_number, is_insurance_verified, "
                + "health_declaration_status, vaccination_details, special_needs_details, emergency_contact_phone "
                + "FROM traveler_compliance_records WHERE traveler_passport_number = ? AND departure_id = ?",
                india.getPassportNumber(), departure.getId());
        assertEquals("Ceylinco", record.get("INSURANCE_PROVIDER"));
        assertEquals(Boolean.FALSE, record.get("IS_INSURANCE_VERIFIED"));
        assertEquals("SUBMITTED", record.get("HEALTH_DECLARATION_STATUS"));
        assertEquals("Wheelchair at airports", String.valueOf(record.get("SPECIAL_NEEDS_DETAILS")));

        incidentService.log(departure.getId(), india.getId(), IncidentType.LOST_PASSPORT, IncidentSeverity.HIGH,
                "Passport lost at the hotel", null, "operator");
        Map<String, Object> incident = jdbc.queryForMap("SELECT incident_type, severity, status, affected_passport_number, "
                + "reported_by_staff_id, action_taken FROM tour_incidents WHERE departure_id = ?", departure.getId());
        assertEquals("LOST_PASSPORT", incident.get("INCIDENT_TYPE"));
        assertEquals("OPEN", incident.get("STATUS"));
        assertEquals(india.getPassportNumber(), incident.get("AFFECTED_PASSPORT_NUMBER"));
        assertTrue(String.valueOf(incident.get("ACTION_TAKEN")).contains("police report"));
    }

    @Test
    void visaRulesUseTheTeamColumns() {
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM visa_types WHERE visa_name = 'eTOURIST VISA' "
                + "AND standard_processing_days > 0 AND fee >= 0", Integer.class) > 0);
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM visa_requirement_rules WHERE document_name IS NOT NULL "
                + "AND accepted_formats IS NOT NULL AND max_size_mb > 0 AND is_mandatory = TRUE", Integer.class) > 0);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM supported_countries WHERE country_code IN ('IN', 'LK')", Integer.class));
    }

    @Test
    void noTablesBesideTheTeamSchemaExceptTheDocumentedAdditions() throws Exception {
        List<String> tables = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), c.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        List<String> team = List.of("admin_users", "supported_countries", "customer_accounts", "visa_types", "visa_requirement_rules",
                "tour_departures", "india_visa_applications", "srilanka_visa_applications", "india_visa_application_documents",
                "visa_status_history", "traveler_compliance_records", "tour_incidents");
        // On MySQL the two id generators are small tables; on H2 they are sequences
        List<String> additions = List.of("auth_audit_log", "srilanka_visa_application_documents",
                "visa_application_seq", "visa_document_seq",
                // Module 1: website traveller accounts
                "site_customers",
                // Module 3: billing
                "invoice", "payment", "payment_slip",
                // Module 4: logistics
                "departure", "supplier", "hotel_booking", "transport", "guide", "assignment", "supplier_payment",
                // Module 5: tour packages and website bookings
                "tour_package", "itinerary", "tour_booking");
        for (String table : tables) {
            assertTrue(team.contains(table) || additions.contains(table), "unexpected table " + table);
        }
        assertTrue(tables.containsAll(team));
    }

    @Test
    void teamColumnDefinitionsAreNotChanged() throws Exception {
        // Hibernate would widen/shrink these to VARCHAR(255) without TeamSchema*Dialect
        assertEquals(10, columnSize("india_visa_applications", "changed_name"));
        assertEquals(500, columnSize("india_visa_applications", "approved_visa_file_path"));
        assertEquals(500, columnSize("india_visa_application_documents", "file_path"));
        assertEquals(50, columnSize("admin_users", "role"));
        assertEquals(100, columnSize("customer_accounts", "reference_number"));
    }

    private int columnSize(String table, String column) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            for (String t : List.of(table, table.toUpperCase())) {
                try (ResultSet rs = meta.getColumns(c.getCatalog(), c.getSchema(), t, null)) {
                    while (rs.next()) {
                        if (rs.getString("COLUMN_NAME").equalsIgnoreCase(column)) return rs.getInt("COLUMN_SIZE");
                    }
                }
            }
        }
        throw new AssertionError("column " + table + "." + column + " not found");
    }
}
