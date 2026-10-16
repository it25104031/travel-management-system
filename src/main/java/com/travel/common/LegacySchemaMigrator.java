package com.travel.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Configuration
public class LegacySchemaMigrator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaMigrator.class);

    private final DataSource dataSource;

    public LegacySchemaMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnLegacyMigrator() {
        return new EntityManagerFactoryDependsOnPostProcessor("legacySchemaMigrator");
    }

    @Override
    public void afterPropertiesSet() {
        try {
            String product;
            try (Connection c = dataSource.getConnection()) {
                product = c.getMetaData().getDatabaseProductName();
            }
            if (product == null || !product.toLowerCase().contains("mysql")) return;
            migrate(new JdbcTemplate(dataSource));
        } catch (Exception e) {
            log.warn("Legacy schema upgrade skipped: {}", e.getMessage());
        }
    }

    private void migrate(JdbcTemplate jdbc) {
        if (tableExists(jdbc, "auth_audit_log")) {
            List<String> types = jdbc.queryForList(
                    "SELECT data_type FROM information_schema.columns WHERE table_schema = DATABASE() "
                            + "AND table_name = 'auth_audit_log' AND column_name = 'event_type'", String.class);
            if (!types.isEmpty() && "enum".equalsIgnoreCase(types.get(0))) {
                jdbc.execute("ALTER TABLE auth_audit_log MODIFY event_type VARCHAR(40) NOT NULL");
                log.info("Converted auth_audit_log.event_type from ENUM to VARCHAR(40)");
            }
        }
        migratePaymentSlips(jdbc);
    }

    private void migratePaymentSlips(JdbcTemplate jdbc) {
        if (!tableExists(jdbc, "payment_slip")) return;
        List<String> nullable = jdbc.queryForList(
                "SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'payment_slip' AND column_name = 'user_id'", String.class);
        if (!nullable.isEmpty() && "NO".equalsIgnoreCase(nullable.get(0))) {
            jdbc.execute("ALTER TABLE payment_slip MODIFY user_id BIGINT NULL");
            log.info("Made payment_slip.user_id optional (slips can now come from website travellers)");
        }
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }
}
