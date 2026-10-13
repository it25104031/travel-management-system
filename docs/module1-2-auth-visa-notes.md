# Authentication & Identity + Visa, Permit & Travel Compliance

## Run it
1. Create `travel_db` with the team's schema, or run `travel_db.sql` (the team schema plus this module's additions).
   MySQL user `root`, no password, as in `application.yml`.
2. `./mvnw spring-boot:run` (or run `TravelApplication` in IntelliJ).
3. Missing columns and tables are added automatically (`ddl-auto: update`); the team's own columns are never
   renamed, resized or dropped (`TeamSchemaMySQLDialect`). Status `DRAFT` rows are read as `PENDING`.
4. Run the tests with `./mvnw test`. They need no MySQL: each run builds an in-memory database from the team
   schema (`src/test/resources/team-schema.sql`) and checks the data lands in the team's tables and columns.

| Who | Where | Login |
|---|---|---|
| Admin | http://localhost:8082/login | `admin` / the password behind the hash in the team's `INSERT` |
| Operator | http://localhost:8082/login | `operator` / `operator123` |
| Applicant | http://localhost:8082/visas/select-country (India or Sri Lanka) | no login |
| Applicant portal | http://localhost:8082/portal/login | reference number + passkey shown after submitting |

Emails (passkeys, rejections, reminders) are printed to the console unless `spring.mail.*` is configured in `application.yml`.

## Team database schema
The code uses the team's `travel_db` tables and column names:

| Team table | Used for | Entity |
|---|---|---|
| `admin_users` | staff accounts (`role` = `ADMIN` / `USER`) | `AdminUserEntity` |
| `customer_accounts` | applicant portal passkeys | `CustomerAccountEntity` |
| `supported_countries` | destinations | `CountryEntity` |
| `visa_types`, `visa_requirement_rules` | visa types, fees, processing days, document rules (size in MB) | `VisaTypeEntity`, `RequiredDocumentEntity` |
| `india_visa_applications`, `srilanka_visa_applications` | one table per destination | `IndiaVisaApplicationEntity`, `SriLankaVisaApplicationEntity` (shared fields in `VisaApplicationEntity`) |
| `india_visa_application_documents` | uploaded files for India | `IndiaVisaApplicationDocumentEntity` |
| `visa_status_history` | every status change (`application_type` = `INDIA` / `SRI_LANKA`) | `ApplicationStatusHistoryEntity` |
| `tour_departures` | group departures, `booking_gate_status`, `readiness_percentage` | `TourDepartureEntity` |
| `traveler_compliance_records` | insurance + health declaration per traveller and departure | `ComplianceRecordEntity` |
| `tour_incidents` | incidents (traveller by passport number, reporter by staff id) | `IncidentEntity` |

What this module adds on top (Part 2 of `travel_db.sql`, created automatically on startup):
- Tables: `auth_audit_log` (sign-in audit), `srilanka_visa_application_documents` (the team schema has no Sri Lanka
  document table), `visa_application_seq` / `visa_document_seq` (application and document ids are shared across the
  India and Sri Lanka tables, so one id always means one application).
- Nullable columns on team tables, e.g. `admin_users.full_name/email/active`, `customer_accounts.application_id`,
  `visa_requirement_rules.doc_code/nationality/min_width_px`, `traveler_compliance_records.*_verified_by`,
  Sri Lanka government-tracking columns that India already has. The full list is in `travel_db.sql`.

Behaviour that follows from the schema:
- Insurance and health details can only be entered once a traveller is on a tour departure (`departure_id` is required).
- Every incident belongs to a departure and is logged by a staff account (`reported_by_staff_id`).
- A passport number can be used once per destination (`passport_number` is unique in each table), including
  after a rejection; staff handle re-applications.
- Every departure has a destination country, and only applicants for that country can be added to it.

## Module 1: Authentication & Identity (`com.travel.module1_auth`)
| Requirement | Where |
|---|---|
| Staff accounts with full name, email, active flag, timestamps | `AdminUserEntity` |
| Provision, change role, deactivate (no delete), forced password reset | `/admin/users`, `AdminUserService` |
| Change own password (validates current one) | `/account/password`, `AccountController` |
| Temporary customer credentials on submission (BCrypt-hashed, SecureRandom passkey, expiry) | `CustomerAccountEntity`, `TempCredentialService` |
| Customer portal login with reference + passkey (`ROLE_CUSTOMER`) | `SecurityConfig` chain 1, `CustomerAuthenticationProvider` |
| Expired passkey: refused with renewal prompt; "email me a new passkey" | `/portal/renew` |
| Session lifetime: staff 30 min idle, customers 15 min idle, customer session cut when account closes or expires | `CustomLoginSuccessHandler`, `CustomerLoginSuccessHandler`, `CustomerSessionValidationFilter` |
| RBAC: `/admin/**` ADMIN, `/visas/**` `/files/**` ADMIN+USER, `/portal/**` CUSTOMER | `SecurityConfig` |
| Auth audit log (logins, failures, logouts, password changes, account events, IP) | `AuthAuditLogEntity`, `/admin/audit` |
| Idle timeouts logged as `SESSION_EXPIRED` (explicit logouts are not double-logged) | `SessionAuditConfig` |
| Lockout after 5 failed attempts in 15 minutes | `AuthAuditService.isLockedOut` |
| BCrypt strength 12 | `PasswordConfig` |
| Customer data isolation (portal never takes an application id from the URL) | `CustomerPortalController` |

## Module 2: Visa, Permit & Travel Compliance (`com.travel.module2_visa`)
| Requirement | Where |
|---|---|
| Destination countries (add, activate/deactivate) | `/admin/countries` |
| Visa types, fees, processing days, required documents per nationality | `/admin/visa-rules`, `VisaTypeEntity`, `RequiredDocumentEntity` |
| Application forms per destination: India (`/visas/apply/india`) and Sri Lanka ETA (`/visas/apply/sri-lanka`) | `CountryForm`, `VisaApplyController` |
| Dynamic upload slots on the application form | choose type + nationality first |
| Passport unique per destination; visa type must belong to the chosen destination; reference prefix per country (`IN-`, `LK-`) | `VisaApplicationService.submit` |
| Upload validation: format, size, minimum photo dimensions | `DocumentStorageService.validate` |
| Verify / reject documents with mandatory reason, re-upload with version history | review page, `VisaApplicationService` |
| Status workflow PENDING, SUBMITTED, APPROVED, REJECTED with guards and immutable history | `changeStatus`, `ApplicationStatusHistoryEntity` |
| Government reference, submission time, grant date, validity, entry type, approved visa PDF | review page "Government reference tracking" |
| Passport 6-month rule | `PassportValidityService` (badges on dashboard and review page) |
| Passport masking for operators, audited "reveal" | `MaskingUtil`, `VisaReviewController` |
| Operator queue: assignment, overdue flag, sorted by travel date | `/visas/dashboard`, `VisaQueueService` |
| Departure groups, add travellers, copy shared address/itinerary | `/visas/departures`, `DepartureService` |
| Batch status change for selected travellers (all or nothing: if one fails the rules, none change) | departure page, `DepartureService.batchChangeStatus` |
| Insurance, vaccinations, special needs and emergency contact (customer fills, operator verifies) | `/portal/compliance`, review page, `ComplianceService` |
| Readiness % and booking gate (READY only at 100%) | `ReadinessService` |
| Incidents; lost passport invalidates the record, re-locks the gate, emails a checklist | `/visas/incidents`, `IncidentService` |
| Daily jobs: passport/insurance warnings, re-upload reminders, close expired customer accounts | `ComplianceScheduler` |
| Manifests as PDF and CSV | `/visas/departures/{id}/manifest.pdf` / `.csv` |
| Reports: approval rate, average processing time, incidents | `/admin/reports` |
| Transactions: file writes roll back with the DB; file deletes happen after commit | `DocumentStorageService` |
| Uploaded files only served to signed-in staff or the owning customer | `FileController` (the old public `/uploads/**` mapping is gone) |

## Adding another destination country
1. Create its application table (and a documents table) with the team, e.g. `thailand_visa_applications`.
2. Add `ThailandVisaApplicationEntity extends VisaApplicationEntity` and `ThailandVisaApplicationDocumentEntity
   extends ApplicationDocumentEntity`, copying the Sri Lanka ones and adding the destination's own columns.
3. Add a line to `CountryForm`, e.g. `THAILAND("thailand", "TH", "visa-apply-thailand", ThailandVisaApplicationEntity::new)`.
4. Copy `templates/visa-apply-sri-lanka.html` to `visa-apply-thailand.html` and adjust the fields.
5. Add the country, its visa types and document rules in `/admin/countries` and `/admin/visa-rules`.
Review, documents, portal, readiness, departures, manifests and reports then work without changes.

## Not done yet
- Real email sending: messages are printed to the console until `spring.mail.*` is configured.
- Performance targets from the report have not been measured.
