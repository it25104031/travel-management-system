package com.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Travel Agency ERP - one Spring Boot application, one MySQL database (travel_erp).
 *
 *   module1_auth       Authentication & identity (staff, travellers, visa-portal passkeys)  IT25103339
 *   module2_visa       Visa & travel compliance                                             IT25103339
 *   module3_billing    Billing, invoices, payments, bank slips                              IT25104031
 *   module4_logistics  Hotel, transport, guides and supplier payments                       IT25104030
 *   module5_packages   Tour packages, itineraries and website bookings                      IT25104033
 *   web                Public website, cart, checkout, traveller profile, staff home
 *   common             Shared helpers
 *
 * Lives in the root package so component, entity and repository scanning picks up every module.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelApplication.class, args);
    }
}
