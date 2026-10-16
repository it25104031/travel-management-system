CREATE TABLE admin_users (active bit, must_change_password bit, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), username varchar(100) not null, email varchar(255), full_name varchar(255), password varchar(255) not null, role varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE assignment (assignment_id integer not null auto_increment, cost decimal(10,2) not null, departure_id integer not null, end_date date not null, guide_id integer not null, start_date date not null, primary key (assignment_id)) engine=InnoDB;

CREATE TABLE auth_audit_log (created_at datetime(6) not null, id bigint not null auto_increment, event_type varchar(40) not null, ip_address varchar(64), details varchar(500), username varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE customer_accounts (is_active bit, application_id bigint, created_at datetime(6), expires_at datetime(6) not null, id bigint not null auto_increment, last_login_at datetime(6), customer_email varchar(255) not null, passkey_hash varchar(255) not null, reference_number varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE departure (departure_id integer not null auto_increment, end_date date not null, start_date date not null, reference varchar(20) not null, destination varchar(100) not null, package_name varchar(100) not null, primary key (departure_id)) engine=InnoDB;

CREATE TABLE guide (daily_rate decimal(10,2) not null, guide_id integer not null auto_increment, supplier_id integer not null, role varchar(20) not null, phone varchar(30), name varchar(100) not null, primary key (guide_id)) engine=InnoDB;

CREATE TABLE hotel_booking (check_in date not null, check_out date not null, cost decimal(10,2) not null, departure_id integer not null, hotel_booking_id integer not null auto_increment, rooms integer not null, supplier_id integer not null, primary key (hotel_booking_id)) engine=InnoDB;

CREATE TABLE india_visa_application_documents (expiry_date date, version_number integer, file_size bigint, id bigint not null, previous_version_id bigint, reviewed_at datetime(6), uploaded_at datetime(6), visa_application_id bigint not null, file_path varchar(500) not null, document_type varchar(255), file_name varchar(255) not null, file_type varchar(255), rejection_reason TEXT, reviewed_by varchar(255), uploaded_by varchar(255), verification_status varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE india_visa_applications (date_of_birth date, expected_date_of_arrival date, gov_approval_date date, gov_submission_date date, passport_date_of_expiry date, passport_date_of_issue date, passport_invalidated bit, visa_validity_end date, visa_validity_start date, assigned_operator_id bigint, created_at datetime(6), departure_id bigint, id bigint not null, site_customer_id bigint, visa_type_id bigint, approved_visa_file_path varchar(500), acquired_nationality_by varchar(255), arrested_or_prosecuted varchar(255), booked_through_tour_operator varchar(255), changed_name varchar(255), changed_name_details varchar(255), cities_previously_visited varchar(255), citizenship_national_id_no varchar(255), countries_visited_last_10_years varchar(255), country_of_birth varchar(255), educational_qualification varchar(255), email_id varchar(255), engaged_in_terrorism_or_cyber_crime varchar(255), engaged_in_trafficking_or_fraud varchar(255), entry_type varchar(255), expected_port_of_exit varchar(255), expressed_glorifying_views varchar(255), father_country_of_birth varchar(255), father_name varchar(255), father_nationality varchar(255), father_place_of_birth varchar(255), father_previous_nationality varchar(255), gender varchar(255), given_names varchar(255), gov_portal_ref_no varchar(255), grandparents_pakistan_origin varchar(255), last_indian_visa_no varchar(255), last_visa_date_of_issue varchar(255), last_visa_place_of_issue varchar(255), last_visa_type varchar(255), lived_in_country_two_years varchar(255), marital_status varchar(255), mother_country_of_birth varchar(255), mother_name varchar(255), mother_nationality varchar(255), mother_place_of_birth varchar(255), mother_previous_nationality varchar(255), nationality varchar(255), other_passport_country varchar(255), other_passport_held varchar(255), other_passport_issue_date varchar(255), other_passport_nationality varchar(255), other_passport_no varchar(255), other_passport_place varchar(255), pakistan_origin_details varchar(255), passport_number varchar(255) not null, passport_place_of_issue varchar(255), passport_type varchar(255), permanent_house_no varchar(255), permanent_state_province_district varchar(255), permanent_village_town_city varchar(255), permission_refused varchar(255), places_to_be_visited_line_1 varchar(255), places_to_be_visited_line_2 varchar(255), port_of_arrival varchar(255), present_country varchar(255), present_house_no varchar(255), present_mobile_no varchar(255), present_phone_no varchar(255), present_postal_zip_code varchar(255), present_state_province_district varchar(255), present_village_town_city varchar(255), previous_address_in_india varchar(255), reference_address_in_home_country varchar(255), reference_address_in_india varchar(255), reference_name_in_home_country varchar(255), reference_name_in_india varchar(255), reference_phone_in_home_country varchar(255), reference_phone_in_india varchar(255), refusal_details varchar(255), refused_entry_or_deported varchar(255), religion varchar(255), sought_asylum varchar(255), status varchar(255), surname varchar(255), town_city_of_birth varchar(255), visa_service varchar(255), visible_identification_marks varchar(255), visited_india_before varchar(255), visited_saarc_countries varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE invoice (invoice_id integer not null auto_increment, total_amount decimal(10,2) not null, invoice_date datetime(6) not null, customer_name varchar(255) not null, invoice_number varchar(255) not null, invoice_status varchar(255) not null, primary key (invoice_id)) engine=InnoDB;

CREATE TABLE itinerary (day_number integer not null, itinerary_id integer not null auto_increment, package_id integer not null, destination varchar(100), activities varchar(1000), accommodation varchar(255), transport varchar(255), primary key (itinerary_id)) engine=InnoDB;

CREATE TABLE payment (amount decimal(10,2) not null, invoice_id integer not null, payment_id integer not null auto_increment, payment_date datetime(6) not null, payment_method varchar(255) not null, payment_status varchar(255) not null, primary key (payment_id)) engine=InnoDB;

CREATE TABLE payment_slip (amount_paid decimal(10,2) not null, invoice_id integer not null, slip_id integer not null auto_increment, created_at datetime(6) not null, customer_id bigint, file_size bigint not null, payment_date datetime(6) not null, user_id bigint, admin_remark TEXT, bank_reference varchar(255), content_type varchar(255) not null, original_file_name varchar(255) not null, status varchar(255) not null, stored_file_name varchar(255) not null, primary key (slip_id)) engine=InnoDB;

CREATE TABLE site_customers (is_active bit not null, created_at datetime(6), id bigint not null auto_increment, last_login_at datetime(6), phone varchar(30), full_name varchar(120) not null, email varchar(190) not null, password_hash varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE srilanka_visa_application_documents (expiry_date date, version_number integer, file_size bigint, id bigint not null, previous_version_id bigint, reviewed_at datetime(6), uploaded_at datetime(6), visa_application_id bigint not null, file_path varchar(500) not null, document_type varchar(255), file_name varchar(255) not null, file_type varchar(255), rejection_reason TEXT, reviewed_by varchar(255), uploaded_by varchar(255), verification_status varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE srilanka_visa_applications (date_of_birth date, gov_approval_date date, gov_submission_date date, intended_arrival_date date, passport_date_of_expiry date, passport_date_of_issue date, passport_invalidated bit, visa_validity_end date, visa_validity_start date, assigned_operator_id bigint, created_at datetime(6), departure_id bigint, id bigint not null, site_customer_id bigint, visa_type_id bigint, approved_visa_file_path varchar(500), contact_number varchar(255), email_id varchar(255), entry_type varchar(255), gender varchar(255), given_names varchar(255), gov_portal_ref_no varchar(255), nationality varchar(255), passport_number varchar(255) not null, port_of_arrival varchar(255), purpose_of_visit varchar(255), status varchar(255), surname varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE supplier (contract_end date not null, contract_start date not null, room_capacity integer, supplier_id integer not null auto_increment, type varchar(20) not null, contact_phone varchar(30), name varchar(100) not null, primary key (supplier_id)) engine=InnoDB;

CREATE TABLE supplier_payment (amount decimal(10,2) not null, departure_id integer not null, payment_date date not null, supplier_id integer not null, supplier_payment_id integer not null auto_increment, reference varchar(60), primary key (supplier_payment_id)) engine=InnoDB;

CREATE TABLE supported_countries (is_active bit, id bigint not null auto_increment, country_code varchar(255) not null, country_name varchar(255) not null, form_route varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE tour_booking (booking_id integer not null auto_increment, invoice_id integer, package_id integer not null, total_amount decimal(12,2) not null, travel_date date not null, travellers integer not null, unit_price decimal(12,2) not null, created_at datetime(6) not null, customer_id bigint not null, booking_ref varchar(20) not null, status varchar(20) not null, notes varchar(500), primary key (booking_id)) engine=InnoDB;

CREATE TABLE tour_departures (departure_date date not null, readiness_percentage decimal(5,2), return_date date not null, created_at datetime(6), id bigint not null auto_increment, ready_at datetime(6), booking_gate_status varchar(255), departure_code varchar(255) not null, destination_country varchar(255) not null, ready_by varchar(255), tour_name varchar(255) not null, primary key (id)) engine=InnoDB;

CREATE TABLE tour_incidents (departure_id bigint not null, id bigint not null auto_increment, reported_at datetime(6), reported_by_staff_id bigint not null, resolved_at datetime(6), action_taken TEXT, affected_passport_number varchar(255), assigned_to varchar(255), description TEXT not null, incident_type varchar(255) not null, resolution TEXT, severity varchar(255) not null, status varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE tour_package (duration integer not null, package_id integer not null auto_increment, price decimal(12,2) not null, status varchar(30) not null, category varchar(40), destination varchar(100), package_name varchar(150) not null, image_url varchar(500), description varchar(1000), primary key (package_id)) engine=InnoDB;

CREATE TABLE transport (cost decimal(10,2) not null, departure_id integer not null, seats integer not null, service_date date not null, supplier_id integer not null, transport_id integer not null auto_increment, mode varchar(20) not null, vehicle_reference varchar(60) not null, primary key (transport_id)) engine=InnoDB;

CREATE TABLE traveler_compliance_records (fit_to_travel bit, insurance_end_date date, insurance_start_date date, is_insurance_verified bit, departure_id bigint not null, health_verified_at datetime(6), id bigint not null auto_increment, insurance_verified_at datetime(6), emergency_contact_name varchar(255), emergency_contact_phone varchar(255), health_declaration_status varchar(255), health_rejection_reason varchar(255), health_verified_by varchar(255), insurance_policy_number varchar(255), insurance_provider varchar(255), insurance_rejection_reason varchar(255), insurance_verified_by varchar(255), special_needs_details TEXT, traveler_passport_number varchar(255) not null, vaccination_details TEXT, primary key (id)) engine=InnoDB;

CREATE TABLE visa_application_seq (next_val bigint) engine=InnoDB;

insert into visa_application_seq ( next_val ) values ( 1 );

CREATE TABLE visa_document_seq (next_val bigint) engine=InnoDB;

insert into visa_document_seq ( next_val ) values ( 1 );

CREATE TABLE visa_requirement_rules (is_mandatory bit, max_size_mb integer, min_height_px integer, min_width_px integer, id bigint not null auto_increment, visa_type_id bigint not null, accepted_formats varchar(255), doc_code varchar(255), document_name varchar(255) not null, nationality varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE visa_status_history (application_id bigint not null, changed_at datetime(6) not null, id bigint not null auto_increment, application_type varchar(255) not null, change_reason TEXT, changed_by_user varchar(255) not null, new_status varchar(255) not null, previous_status varchar(255), primary key (id)) engine=InnoDB;

CREATE TABLE visa_types (active bit, fee decimal(10,2) not null, standard_processing_days integer not null, country_id bigint not null, id bigint not null auto_increment, category varchar(255), currency varchar(255), visa_name varchar(255) not null, primary key (id)) engine=InnoDB;

ALTER TABLE admin_users add constraint UK3fgxk4ewgaxgtgvqwb1jjudj6 unique (username);

ALTER TABLE customer_accounts add constraint UKksvkvu1wooegonf3xdsd5h2y2 unique (application_id);

ALTER TABLE customer_accounts add constraint UKnjnbdqdhctnm0yv2ubgam43de unique (reference_number);

ALTER TABLE departure add constraint UKqhw5fgdtg47pb0v8veuqmgyvu unique (reference);

ALTER TABLE india_visa_applications add constraint UK34iajv39yjb45fhakhk2ed41r unique (passport_number);

ALTER TABLE invoice add constraint UKt6xkdjx1qtd5whp2iljdfn2yj unique (invoice_number);

ALTER TABLE itinerary add constraint uq_package_day unique (package_id, day_number);

ALTER TABLE site_customers add constraint UK83uvu8wwr40aaj4lcn94bubdo unique (email);

ALTER TABLE srilanka_visa_applications add constraint UK34iajv39yjb45fhakhk2ed41r unique (passport_number);

ALTER TABLE supported_countries add constraint UKkh12x9uees75nxgqhq7lxi8n7 unique (country_code);

ALTER TABLE supported_countries add constraint UKt08rp25cposr1de9rqer60al8 unique (country_name);

ALTER TABLE tour_booking add constraint UKhocjcky38oqvs9l6m9n0smsfq unique (booking_ref);

ALTER TABLE tour_departures add constraint UKri5pso78s1jahafl2sfm1k97t unique (departure_code);

ALTER TABLE assignment add constraint FKt0j1w7qhqlwtn2m35kyssrrlw foreign key (departure_id) references departure (departure_id);

ALTER TABLE assignment add constraint FKra6ibsjhg9kkft1rmxw8c68kv foreign key (guide_id) references guide (guide_id);

ALTER TABLE guide add constraint FK4rfpc9i3lwv17i1c9ivs7l5uh foreign key (supplier_id) references supplier (supplier_id);

ALTER TABLE hotel_booking add constraint FKq6or6fhohmg962hrxxyvuel1i foreign key (departure_id) references departure (departure_id);

ALTER TABLE hotel_booking add constraint FKej6rx5et80othnig1wagysxrq foreign key (supplier_id) references supplier (supplier_id);

ALTER TABLE india_visa_application_documents add constraint FKbwruaekn37cfoj2u5cpjr7pnl foreign key (visa_application_id) references india_visa_applications (id);

ALTER TABLE india_visa_applications add constraint FK26rj1dtkgo0qkbfh13grqrw06 foreign key (assigned_operator_id) references admin_users (id);

ALTER TABLE india_visa_applications add constraint FKt1i5p7lc0kqw8cpkyhq9bygsr foreign key (departure_id) references tour_departures (id);

ALTER TABLE itinerary add constraint FKej9g69iqv3ywf68w1drlj91qh foreign key (package_id) references tour_package (package_id);

ALTER TABLE payment add constraint FKsb24p8f52refbb80qwp4gem9n foreign key (invoice_id) references invoice (invoice_id);

ALTER TABLE payment_slip add constraint FK2pm9f7a1qqa2ae6y88locnms3 foreign key (customer_id) references site_customers (id);

ALTER TABLE payment_slip add constraint FKmrayvoj2m6tbuvemeii99nlt9 foreign key (invoice_id) references invoice (invoice_id);

ALTER TABLE payment_slip add constraint FKg7pxrska126uj4c0sinqbvovw foreign key (user_id) references admin_users (id);

ALTER TABLE srilanka_visa_application_documents add constraint FKkg9se6q4yghthiwdty9rp8m1t foreign key (visa_application_id) references srilanka_visa_applications (id);

ALTER TABLE srilanka_visa_applications add constraint FKpfja08qt8pvc932760p18i9wq foreign key (assigned_operator_id) references admin_users (id);

ALTER TABLE srilanka_visa_applications add constraint FKc5v4ar9jdhobijgj3a0416elo foreign key (departure_id) references tour_departures (id);

ALTER TABLE supplier_payment add constraint FKfwf8imj1tdyelljpy7kpqkjwf foreign key (departure_id) references departure (departure_id);

ALTER TABLE supplier_payment add constraint FKqy7og5c3psvi8j5yn2lmrki7x foreign key (supplier_id) references supplier (supplier_id);

ALTER TABLE tour_booking add constraint FKk9kft5pnspc0bxoij0jj0kw7q foreign key (customer_id) references site_customers (id);

ALTER TABLE tour_booking add constraint FKsfdlo7if6mt1nhvdnehqiwhbp foreign key (package_id) references tour_package (package_id);

ALTER TABLE tour_incidents add constraint FK6gauv5sje1etvvksgl3i2obwt foreign key (departure_id) references tour_departures (id);

ALTER TABLE tour_incidents add constraint FK8ip9x7a4ff6o293w4mc9kla53 foreign key (reported_by_staff_id) references admin_users (id);

ALTER TABLE transport add constraint FK8my9cb3g4ci6bwstdpmll7rxq foreign key (departure_id) references departure (departure_id);

ALTER TABLE transport add constraint FK67ppi6aqjvi8awk497hjdr6nk foreign key (supplier_id) references supplier (supplier_id);

ALTER TABLE traveler_compliance_records add constraint FKsdk7o49b25l5wg8tdccieb2k0 foreign key (departure_id) references tour_departures (id);

ALTER TABLE visa_requirement_rules add constraint FKfa7i8tgxf9xsa3oevh0vjiufk foreign key (visa_type_id) references visa_types (id);

ALTER TABLE visa_types add constraint FKb103385rxrmt8tej75ge5ktk2 foreign key (country_id) references supported_countries (id);
