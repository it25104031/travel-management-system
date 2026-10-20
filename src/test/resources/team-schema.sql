CREATE TABLE admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'ADMIN'
);

INSERT INTO admin_users (username, password, role)
VALUES ('admin', '$2a$10$XGZ9grESLT0UzMW1Wh8JNOBl23CARt700gSYiq/pqZvQelzYzPhVa', 'ADMIN');

CREATE TABLE supported_countries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_name VARCHAR(100) NOT NULL UNIQUE,
    country_code VARCHAR(10) NOT NULL UNIQUE,
    form_route VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO supported_countries (country_name, country_code, form_route)
VALUES ('India', 'IN', '/visas/apply/india');

CREATE TABLE customer_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    passkey_hash VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE visa_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_id BIGINT NOT NULL,
    visa_name VARCHAR(100) NOT NULL,
    fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    standard_processing_days INT NOT NULL DEFAULT 5,
    CONSTRAINT fk_visa_type_country
        FOREIGN KEY (country_id) REFERENCES supported_countries(id) ON DELETE CASCADE
);

CREATE TABLE visa_requirement_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visa_type_id BIGINT NOT NULL,
    document_name VARCHAR(150) NOT NULL,
    accepted_formats VARCHAR(100) DEFAULT 'JPG,PNG,PDF',
    max_size_mb INT DEFAULT 5,
    is_mandatory BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_rule_visa_type
        FOREIGN KEY (visa_type_id) REFERENCES visa_types(id) ON DELETE CASCADE
);

CREATE TABLE tour_departures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    departure_code VARCHAR(50) NOT NULL UNIQUE,
    tour_name VARCHAR(200) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE NOT NULL,
    readiness_percentage DECIMAL(5, 2) DEFAULT 0.00,
    booking_gate_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE india_visa_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    departure_id BIGINT NULL,
    assigned_operator_id BIGINT NULL,
    gov_portal_ref_no VARCHAR(100) NULL,
    gov_submission_date DATE NULL,
    gov_approval_date DATE NULL,
    visa_validity_start DATE NULL,
    visa_validity_end DATE NULL,
    entry_type VARCHAR(50) NULL,
    approved_visa_file_path VARCHAR(500) NULL,
    passport_type VARCHAR(50),
    nationality VARCHAR(100),
    port_of_arrival VARCHAR(100),
    date_of_birth DATE,
    email_id VARCHAR(255),
    expected_date_of_arrival DATE,
    visa_service VARCHAR(100),
    surname VARCHAR(100),
    given_names VARCHAR(150),
    changed_name VARCHAR(10),
    changed_name_details VARCHAR(255),
    gender VARCHAR(20),
    town_city_of_birth VARCHAR(100),
    country_of_birth VARCHAR(100),
    citizenship_national_id_no VARCHAR(100),
    religion VARCHAR(100),
    visible_identification_marks VARCHAR(255),
    educational_qualification VARCHAR(100),
    acquired_nationality_by VARCHAR(50),
    lived_in_country_two_years VARCHAR(10),
    passport_number VARCHAR(100) NOT NULL UNIQUE,
    passport_place_of_issue VARCHAR(100),
    passport_date_of_issue DATE,
    passport_date_of_expiry DATE,
    other_passport_held VARCHAR(10),
    other_passport_country VARCHAR(100),
    other_passport_no VARCHAR(100),
    other_passport_issue_date VARCHAR(50),
    other_passport_place VARCHAR(100),
    other_passport_nationality VARCHAR(100),
    present_house_no VARCHAR(100),
    present_village_town_city VARCHAR(100),
    present_country VARCHAR(100),
    present_state_province_district VARCHAR(100),
    present_postal_zip_code VARCHAR(50),
    present_phone_no VARCHAR(50),
    present_mobile_no VARCHAR(50),
    permanent_house_no VARCHAR(100),
    permanent_village_town_city VARCHAR(100),
    permanent_state_province_district VARCHAR(100),
    father_name VARCHAR(150),
    father_nationality VARCHAR(100),
    father_previous_nationality VARCHAR(100),
    father_place_of_birth VARCHAR(100),
    father_country_of_birth VARCHAR(100),
    mother_name VARCHAR(150),
    mother_nationality VARCHAR(100),
    mother_previous_nationality VARCHAR(100),
    mother_place_of_birth VARCHAR(100),
    mother_country_of_birth VARCHAR(100),
    marital_status VARCHAR(50),
    grandparents_pakistan_origin VARCHAR(10),
    pakistan_origin_details VARCHAR(255),
    places_to_be_visited_line_1 VARCHAR(255),
    places_to_be_visited_line_2 VARCHAR(255),
    booked_through_tour_operator VARCHAR(10),
    expected_port_of_exit VARCHAR(100),
    visited_india_before VARCHAR(10),
    previous_address_in_india TEXT,
    cities_previously_visited VARCHAR(255),
    last_indian_visa_no VARCHAR(100),
    last_visa_type VARCHAR(100),
    last_visa_place_of_issue VARCHAR(100),
    last_visa_date_of_issue VARCHAR(50),
    permission_refused VARCHAR(10),
    refusal_details TEXT,
    countries_visited_last_10_years TEXT,
    visited_saarc_countries VARCHAR(10),
    reference_name_in_india VARCHAR(150),
    reference_address_in_india TEXT,
    reference_phone_in_india VARCHAR(50),
    reference_name_in_home_country VARCHAR(150),
    reference_address_in_home_country TEXT,
    reference_phone_in_home_country VARCHAR(50),
    arrested_or_prosecuted VARCHAR(10),
    refused_entry_or_deported VARCHAR(10),
    engaged_in_trafficking_or_fraud VARCHAR(10),
    engaged_in_terrorism_or_cyber_crime VARCHAR(10),
    expressed_glorifying_views VARCHAR(10),
    sought_asylum VARCHAR(10),
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_india_departure
        FOREIGN KEY (departure_id) REFERENCES tour_departures(id) ON DELETE SET NULL,
    CONSTRAINT fk_india_operator
        FOREIGN KEY (assigned_operator_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

CREATE TABLE srilanka_visa_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    departure_id BIGINT NULL,
    assigned_operator_id BIGINT NULL,
    given_names VARCHAR(150),
    surname VARCHAR(100),
    nationality VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(20),
    passport_number VARCHAR(100) NOT NULL UNIQUE,
    passport_date_of_issue DATE,
    passport_date_of_expiry DATE,
    intended_arrival_date DATE,
    purpose_of_visit VARCHAR(100),
    port_of_arrival VARCHAR(100),
    email_id VARCHAR(255),
    contact_number VARCHAR(50),
    gov_portal_ref_no VARCHAR(100) NULL,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sl_departure
        FOREIGN KEY (departure_id) REFERENCES tour_departures(id) ON DELETE SET NULL,
    CONSTRAINT fk_sl_operator
        FOREIGN KEY (assigned_operator_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

CREATE TABLE india_visa_application_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visa_application_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_path VARCHAR(500) NOT NULL,
    document_type VARCHAR(100) DEFAULT 'OTHER',
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    rejection_reason TEXT NULL,
    expiry_date DATE NULL,
    version_number INT DEFAULT 1,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_india_visa_document
        FOREIGN KEY (visa_application_id) REFERENCES india_visa_applications(id) ON DELETE CASCADE
);

CREATE TABLE visa_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_type VARCHAR(50) NOT NULL,
    application_id BIGINT NOT NULL,
    changed_by_user VARCHAR(100) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE traveler_compliance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    traveler_passport_number VARCHAR(100) NOT NULL,
    departure_id BIGINT NOT NULL,
    insurance_provider VARCHAR(150),
    insurance_policy_number VARCHAR(100),
    insurance_start_date DATE,
    insurance_end_date DATE,
    is_insurance_verified BOOLEAN DEFAULT FALSE,
    health_declaration_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED',
    vaccination_details TEXT,
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(50),
    special_needs_details TEXT,
    CONSTRAINT fk_compliance_departure
        FOREIGN KEY (departure_id) REFERENCES tour_departures(id) ON DELETE CASCADE
);

CREATE TABLE tour_incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'OPEN',
    description TEXT NOT NULL,
    action_taken TEXT,
    resolution TEXT,
    departure_id BIGINT NOT NULL,
    affected_passport_number VARCHAR(100),
    reported_by_staff_id BIGINT NOT NULL,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    CONSTRAINT fk_incident_departure
        FOREIGN KEY (departure_id) REFERENCES tour_departures(id) ON DELETE CASCADE,
    CONSTRAINT fk_incident_staff
        FOREIGN KEY (reported_by_staff_id) REFERENCES admin_users(id) ON DELETE RESTRICT
);
