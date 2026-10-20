CREATE DATABASE IF NOT EXISTS travel_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_erp;

CREATE TABLE IF NOT EXISTS admin_users (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    username              VARCHAR(100) NOT NULL,
    password              VARCHAR(255) NOT NULL,
    role                  VARCHAR(255) NOT NULL,
    full_name             VARCHAR(255),
    email                 VARCHAR(255),
    active                BIT,
    must_change_password  BIT,
    created_at            DATETIME(6),
    updated_at            DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_users_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_audit_log (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    username    VARCHAR(255) NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    ip_address  VARCHAR(64),
    details     VARCHAR(500),
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer_accounts (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    reference_number  VARCHAR(255) NOT NULL,
    passkey_hash      VARCHAR(255) NOT NULL,
    expires_at        DATETIME(6) NOT NULL,
    is_active         BIT,
    application_id    BIGINT,
    customer_email    VARCHAR(255) NOT NULL,
    created_at        DATETIME(6),
    last_login_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_accounts_application_id UNIQUE (application_id),
    CONSTRAINT uk_customer_accounts_reference_number UNIQUE (reference_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS site_customers (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(190) NOT NULL,
    phone          VARCHAR(30),
    password_hash  VARCHAR(255) NOT NULL,
    is_active      BIT NOT NULL,
    created_at     DATETIME(6),
    last_login_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_site_customers_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supported_countries (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    country_name  VARCHAR(255) NOT NULL,
    country_code  VARCHAR(255) NOT NULL,
    form_route    VARCHAR(255) NOT NULL,
    is_active     BIT,
    PRIMARY KEY (id),
    CONSTRAINT uk_supported_countries_country_code UNIQUE (country_code),
    CONSTRAINT uk_supported_countries_country_name UNIQUE (country_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visa_types (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    country_id                BIGINT NOT NULL,
    visa_name                 VARCHAR(255) NOT NULL,
    category                  VARCHAR(255),
    fee                       DECIMAL(10,2) NOT NULL,
    currency                  VARCHAR(255),
    standard_processing_days  INTEGER NOT NULL,
    active                    BIT,
    PRIMARY KEY (id),
    CONSTRAINT fk_visa_types_country_id FOREIGN KEY (country_id) REFERENCES supported_countries (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visa_requirement_rules (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    visa_type_id      BIGINT NOT NULL,
    nationality       VARCHAR(255),
    doc_code          VARCHAR(255),
    document_name     VARCHAR(255) NOT NULL,
    accepted_formats  VARCHAR(255),
    max_size_mb       INTEGER,
    min_width_px      INTEGER,
    min_height_px     INTEGER,
    is_mandatory      BIT,
    PRIMARY KEY (id),
    CONSTRAINT fk_visa_requirement_rules_visa_type_id FOREIGN KEY (visa_type_id) REFERENCES visa_types (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tour_departures (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    departure_code        VARCHAR(255) NOT NULL,
    tour_name             VARCHAR(255) NOT NULL,
    destination_country   VARCHAR(255) NOT NULL,
    departure_date        DATE NOT NULL,
    return_date           DATE NOT NULL,
    booking_gate_status   VARCHAR(255),
    ready_by              VARCHAR(255),
    ready_at              DATETIME(6),
    created_at            DATETIME(6),
    readiness_percentage  DECIMAL(5,2),
    PRIMARY KEY (id),
    CONSTRAINT uk_tour_departures_departure_code UNIQUE (departure_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS india_visa_applications (
    id                                   BIGINT NOT NULL,
    given_names                          VARCHAR(255),
    surname                              VARCHAR(255),
    nationality                          VARCHAR(255),
    date_of_birth                        DATE,
    gender                               VARCHAR(255),
    passport_number                      VARCHAR(255) NOT NULL,
    passport_date_of_issue               DATE,
    passport_date_of_expiry              DATE,
    port_of_arrival                      VARCHAR(255),
    email_id                             VARCHAR(255),
    visa_type_id                         BIGINT,
    gov_portal_ref_no                    VARCHAR(255),
    gov_submission_date                  DATE,
    gov_approval_date                    DATE,
    visa_validity_start                  DATE,
    visa_validity_end                    DATE,
    entry_type                           VARCHAR(255),
    approved_visa_file_path              VARCHAR(500),
    assigned_operator_id                 BIGINT,
    departure_id                         BIGINT,
    passport_invalidated                 BIT,
    site_customer_id                     BIGINT,
    status                               VARCHAR(255),
    created_at                           DATETIME(6),
    passport_type                        VARCHAR(255),
    expected_date_of_arrival             DATE,
    visa_service                         VARCHAR(255),
    changed_name                         VARCHAR(255),
    changed_name_details                 VARCHAR(255),
    town_city_of_birth                   VARCHAR(255),
    country_of_birth                     VARCHAR(255),
    citizenship_national_id_no           VARCHAR(255),
    religion                             VARCHAR(255),
    visible_identification_marks         VARCHAR(255),
    educational_qualification            VARCHAR(255),
    acquired_nationality_by              VARCHAR(255),
    lived_in_country_two_years           VARCHAR(255),
    passport_place_of_issue              VARCHAR(255),
    other_passport_held                  VARCHAR(255),
    other_passport_country               VARCHAR(255),
    other_passport_no                    VARCHAR(255),
    other_passport_issue_date            VARCHAR(255),
    other_passport_place                 VARCHAR(255),
    other_passport_nationality           VARCHAR(255),
    present_house_no                     VARCHAR(255),
    present_village_town_city            VARCHAR(255),
    present_country                      VARCHAR(255),
    present_state_province_district      VARCHAR(255),
    present_postal_zip_code              VARCHAR(255),
    present_phone_no                     VARCHAR(255),
    present_mobile_no                    VARCHAR(255),
    permanent_house_no                   VARCHAR(255),
    permanent_village_town_city          VARCHAR(255),
    permanent_state_province_district    VARCHAR(255),
    father_name                          VARCHAR(255),
    father_nationality                   VARCHAR(255),
    father_previous_nationality          VARCHAR(255),
    father_place_of_birth                VARCHAR(255),
    father_country_of_birth              VARCHAR(255),
    mother_name                          VARCHAR(255),
    mother_nationality                   VARCHAR(255),
    mother_previous_nationality          VARCHAR(255),
    mother_place_of_birth                VARCHAR(255),
    mother_country_of_birth              VARCHAR(255),
    marital_status                       VARCHAR(255),
    grandparents_pakistan_origin         VARCHAR(255),
    pakistan_origin_details              VARCHAR(255),
    places_to_be_visited_line_1          VARCHAR(255),
    places_to_be_visited_line_2          VARCHAR(255),
    booked_through_tour_operator         VARCHAR(255),
    expected_port_of_exit                VARCHAR(255),
    visited_india_before                 VARCHAR(255),
    previous_address_in_india            VARCHAR(255),
    cities_previously_visited            VARCHAR(255),
    last_indian_visa_no                  VARCHAR(255),
    last_visa_type                       VARCHAR(255),
    last_visa_place_of_issue             VARCHAR(255),
    last_visa_date_of_issue              VARCHAR(255),
    permission_refused                   VARCHAR(255),
    refusal_details                      VARCHAR(255),
    countries_visited_last_10_years      VARCHAR(255),
    visited_saarc_countries              VARCHAR(255),
    reference_name_in_india              VARCHAR(255),
    reference_address_in_india           VARCHAR(255),
    reference_phone_in_india             VARCHAR(255),
    reference_name_in_home_country       VARCHAR(255),
    reference_address_in_home_country    VARCHAR(255),
    reference_phone_in_home_country      VARCHAR(255),
    arrested_or_prosecuted               VARCHAR(255),
    refused_entry_or_deported            VARCHAR(255),
    engaged_in_trafficking_or_fraud      VARCHAR(255),
    engaged_in_terrorism_or_cyber_crime  VARCHAR(255),
    expressed_glorifying_views           VARCHAR(255),
    sought_asylum                        VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_india_visa_applications_passport_number UNIQUE (passport_number),
    CONSTRAINT fk_india_visa_applications_assigned_operator_id FOREIGN KEY (assigned_operator_id) REFERENCES admin_users (id),
    CONSTRAINT fk_india_visa_applications_departure_id FOREIGN KEY (departure_id) REFERENCES tour_departures (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS srilanka_visa_applications (
    id                       BIGINT NOT NULL,
    given_names              VARCHAR(255),
    surname                  VARCHAR(255),
    nationality              VARCHAR(255),
    date_of_birth            DATE,
    gender                   VARCHAR(255),
    passport_number          VARCHAR(255) NOT NULL,
    passport_date_of_issue   DATE,
    passport_date_of_expiry  DATE,
    port_of_arrival          VARCHAR(255),
    email_id                 VARCHAR(255),
    visa_type_id             BIGINT,
    gov_portal_ref_no        VARCHAR(255),
    gov_submission_date      DATE,
    gov_approval_date        DATE,
    visa_validity_start      DATE,
    visa_validity_end        DATE,
    entry_type               VARCHAR(255),
    approved_visa_file_path  VARCHAR(500),
    assigned_operator_id     BIGINT,
    departure_id             BIGINT,
    passport_invalidated     BIT,
    site_customer_id         BIGINT,
    status                   VARCHAR(255),
    created_at               DATETIME(6),
    intended_arrival_date    DATE,
    purpose_of_visit         VARCHAR(255),
    contact_number           VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_srilanka_visa_applications_passport_number UNIQUE (passport_number),
    CONSTRAINT fk_srilanka_visa_applications_assigned_operator_id FOREIGN KEY (assigned_operator_id) REFERENCES admin_users (id),
    CONSTRAINT fk_srilanka_visa_applications_departure_id FOREIGN KEY (departure_id) REFERENCES tour_departures (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS india_visa_application_documents (
    id                   BIGINT NOT NULL,
    file_name            VARCHAR(255) NOT NULL,
    file_type            VARCHAR(255),
    file_path            VARCHAR(500) NOT NULL,
    file_size            BIGINT,
    document_type        VARCHAR(255),
    verification_status  VARCHAR(255),
    rejection_reason     TEXT,
    expiry_date          DATE,
    reviewed_by          VARCHAR(255),
    reviewed_at          DATETIME(6),
    version_number       INTEGER,
    previous_version_id  BIGINT,
    uploaded_by          VARCHAR(255),
    uploaded_at          DATETIME(6),
    visa_application_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_india_visa_application_documents_visa_application_id FOREIGN KEY (visa_application_id) REFERENCES india_visa_applications (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS srilanka_visa_application_documents (
    id                   BIGINT NOT NULL,
    file_name            VARCHAR(255) NOT NULL,
    file_type            VARCHAR(255),
    file_path            VARCHAR(500) NOT NULL,
    file_size            BIGINT,
    document_type        VARCHAR(255),
    verification_status  VARCHAR(255),
    rejection_reason     TEXT,
    expiry_date          DATE,
    reviewed_by          VARCHAR(255),
    reviewed_at          DATETIME(6),
    version_number       INTEGER,
    previous_version_id  BIGINT,
    uploaded_by          VARCHAR(255),
    uploaded_at          DATETIME(6),
    visa_application_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_srilanka_visa_application_documents_visa_application_id FOREIGN KEY (visa_application_id) REFERENCES srilanka_visa_applications (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visa_status_history (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    application_type  VARCHAR(255) NOT NULL,
    application_id    BIGINT NOT NULL,
    previous_status   VARCHAR(255),
    new_status        VARCHAR(255) NOT NULL,
    changed_by_user   VARCHAR(255) NOT NULL,
    change_reason     TEXT,
    changed_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS traveler_compliance_records (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    traveler_passport_number    VARCHAR(255) NOT NULL,
    departure_id                BIGINT NOT NULL,
    insurance_provider          VARCHAR(255),
    insurance_policy_number     VARCHAR(255),
    insurance_start_date        DATE,
    insurance_end_date          DATE,
    is_insurance_verified       BIT,
    insurance_verified_by       VARCHAR(255),
    insurance_verified_at       DATETIME(6),
    insurance_rejection_reason  VARCHAR(255),
    health_declaration_status   VARCHAR(255),
    fit_to_travel               BIT,
    vaccination_details         TEXT,
    special_needs_details       TEXT,
    emergency_contact_name      VARCHAR(255),
    emergency_contact_phone     VARCHAR(255),
    health_verified_by          VARCHAR(255),
    health_verified_at          DATETIME(6),
    health_rejection_reason     VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_traveler_compliance_records_departure_id FOREIGN KEY (departure_id) REFERENCES tour_departures (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tour_incidents (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    departure_id              BIGINT NOT NULL,
    affected_passport_number  VARCHAR(255),
    incident_type             VARCHAR(255) NOT NULL,
    severity                  VARCHAR(255) NOT NULL,
    status                    VARCHAR(255),
    description               TEXT NOT NULL,
    resolution                TEXT,
    action_taken              TEXT,
    reported_by_staff_id      BIGINT NOT NULL,
    assigned_to               VARCHAR(255),
    reported_at               DATETIME(6),
    resolved_at               DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tour_incidents_departure_id FOREIGN KEY (departure_id) REFERENCES tour_departures (id),
    CONSTRAINT fk_tour_incidents_reported_by_staff_id FOREIGN KEY (reported_by_staff_id) REFERENCES admin_users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visa_application_seq (
    next_val  BIGINT
) ENGINE=InnoDB;
INSERT INTO visa_application_seq (next_val) SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM visa_application_seq);

CREATE TABLE IF NOT EXISTS visa_document_seq (
    next_val  BIGINT
) ENGINE=InnoDB;
INSERT INTO visa_document_seq (next_val) SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM visa_document_seq);

CREATE TABLE IF NOT EXISTS invoice (
    invoice_id      INTEGER NOT NULL AUTO_INCREMENT,
    invoice_number  VARCHAR(255) NOT NULL,
    customer_name   VARCHAR(255) NOT NULL,
    invoice_date    DATETIME(6) NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    invoice_status  VARCHAR(255) NOT NULL,
    PRIMARY KEY (invoice_id),
    CONSTRAINT uk_invoice_invoice_number UNIQUE (invoice_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment (
    payment_id      INTEGER NOT NULL AUTO_INCREMENT,
    invoice_id      INTEGER NOT NULL,
    payment_date    DATETIME(6) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(255) NOT NULL,
    payment_status  VARCHAR(255) NOT NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_slip (
    slip_id             INTEGER NOT NULL AUTO_INCREMENT,
    invoice_id          INTEGER NOT NULL,
    amount_paid         DECIMAL(10,2) NOT NULL,
    payment_date        DATETIME(6) NOT NULL,
    bank_reference      VARCHAR(255),
    original_file_name  VARCHAR(255) NOT NULL,
    stored_file_name    VARCHAR(255) NOT NULL,
    content_type        VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    status              VARCHAR(255) NOT NULL,
    admin_remark        TEXT,
    created_at          DATETIME(6) NOT NULL,
    customer_id         BIGINT,
    user_id             BIGINT,
    PRIMARY KEY (slip_id),
    CONSTRAINT fk_payment_slip_customer_id FOREIGN KEY (customer_id) REFERENCES site_customers (id),
    CONSTRAINT fk_payment_slip_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id),
    CONSTRAINT fk_payment_slip_user_id FOREIGN KEY (user_id) REFERENCES admin_users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id     INTEGER NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    contact_phone   VARCHAR(30),
    contract_start  DATE NOT NULL,
    contract_end    DATE NOT NULL,
    room_capacity   INTEGER,
    PRIMARY KEY (supplier_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS departure (
    departure_id  INTEGER NOT NULL AUTO_INCREMENT,
    reference     VARCHAR(20) NOT NULL,
    package_name  VARCHAR(100) NOT NULL,
    destination   VARCHAR(100) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    PRIMARY KEY (departure_id),
    CONSTRAINT uk_departure_reference UNIQUE (reference)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS hotel_booking (
    hotel_booking_id  INTEGER NOT NULL AUTO_INCREMENT,
    supplier_id       INTEGER NOT NULL,
    departure_id      INTEGER NOT NULL,
    check_in          DATE NOT NULL,
    check_out         DATE NOT NULL,
    rooms             INTEGER NOT NULL,
    cost              DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (hotel_booking_id),
    CONSTRAINT fk_hotel_booking_departure_id FOREIGN KEY (departure_id) REFERENCES departure (departure_id),
    CONSTRAINT fk_hotel_booking_supplier_id FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport (
    transport_id       INTEGER NOT NULL AUTO_INCREMENT,
    supplier_id        INTEGER NOT NULL,
    departure_id       INTEGER NOT NULL,
    mode               VARCHAR(20) NOT NULL,
    vehicle_reference  VARCHAR(60) NOT NULL,
    service_date       DATE NOT NULL,
    seats              INTEGER NOT NULL,
    cost               DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (transport_id),
    CONSTRAINT fk_transport_departure_id FOREIGN KEY (departure_id) REFERENCES departure (departure_id),
    CONSTRAINT fk_transport_supplier_id FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS guide (
    guide_id     INTEGER NOT NULL AUTO_INCREMENT,
    supplier_id  INTEGER NOT NULL,
    name         VARCHAR(100) NOT NULL,
    role         VARCHAR(20) NOT NULL,
    phone        VARCHAR(30),
    daily_rate   DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (guide_id),
    CONSTRAINT fk_guide_supplier_id FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignment (
    assignment_id  INTEGER NOT NULL AUTO_INCREMENT,
    departure_id   INTEGER NOT NULL,
    guide_id       INTEGER NOT NULL,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    cost           DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (assignment_id),
    CONSTRAINT fk_assignment_departure_id FOREIGN KEY (departure_id) REFERENCES departure (departure_id),
    CONSTRAINT fk_assignment_guide_id FOREIGN KEY (guide_id) REFERENCES guide (guide_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier_payment (
    supplier_payment_id  INTEGER NOT NULL AUTO_INCREMENT,
    supplier_id          INTEGER NOT NULL,
    departure_id         INTEGER NOT NULL,
    payment_date         DATE NOT NULL,
    amount               DECIMAL(10,2) NOT NULL,
    reference            VARCHAR(60),
    PRIMARY KEY (supplier_payment_id),
    CONSTRAINT fk_supplier_payment_departure_id FOREIGN KEY (departure_id) REFERENCES departure (departure_id),
    CONSTRAINT fk_supplier_payment_supplier_id FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tour_package (
    package_id    INTEGER NOT NULL AUTO_INCREMENT,
    duration      INTEGER NOT NULL,
    price         DECIMAL(12,2) NOT NULL,
    status        VARCHAR(30) NOT NULL,
    category      VARCHAR(40),
    destination   VARCHAR(100),
    package_name  VARCHAR(150) NOT NULL,
    image_url     VARCHAR(500),
    description   VARCHAR(1000),
    PRIMARY KEY (package_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS itinerary (
    itinerary_id   INTEGER NOT NULL AUTO_INCREMENT,
    day_number     INTEGER NOT NULL,
    package_id     INTEGER NOT NULL,
    destination    VARCHAR(100),
    activities     VARCHAR(1000),
    accommodation  VARCHAR(255),
    transport      VARCHAR(255),
    PRIMARY KEY (itinerary_id),
    CONSTRAINT uk_itinerary_package_id_day_number UNIQUE (package_id, day_number),
    CONSTRAINT fk_itinerary_package_id FOREIGN KEY (package_id) REFERENCES tour_package (package_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tour_booking (
    booking_id    INTEGER NOT NULL AUTO_INCREMENT,
    booking_ref   VARCHAR(20) NOT NULL,
    customer_id   BIGINT NOT NULL,
    package_id    INTEGER NOT NULL,
    travel_date   DATE NOT NULL,
    travellers    INTEGER NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    total_amount  DECIMAL(12,2) NOT NULL,
    invoice_id    INTEGER,
    status        VARCHAR(20) NOT NULL,
    notes         VARCHAR(500),
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (booking_id),
    CONSTRAINT uk_tour_booking_booking_ref UNIQUE (booking_ref),
    CONSTRAINT fk_tour_booking_customer_id FOREIGN KEY (customer_id) REFERENCES site_customers (id),
    CONSTRAINT fk_tour_booking_package_id FOREIGN KEY (package_id) REFERENCES tour_package (package_id)
) ENGINE=InnoDB;
