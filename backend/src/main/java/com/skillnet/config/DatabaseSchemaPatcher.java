package com.skillnet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes idempotentes que Hibernate ddl-auto=update no aplica (p. ej. relajar NOT NULL).
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DatabaseSchemaPatcher implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        allowNullServiceEntitlementPayment();
    }

    private void allowNullServiceEntitlementPayment() {
        if (!tableExists("core_serviceentitlement")) {
            return;
        }
        if (!columnIsNotNull("core_serviceentitlement", "payment_id")) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE core_serviceentitlement ALTER COLUMN payment_id DROP NOT NULL");
        log.info("Schema patch: core_serviceentitlement.payment_id permite NULL (cuotas IA gratuitas)");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnIsNotNull(String tableName, String columnName) {
        String nullable = jdbcTemplate.queryForObject(
                """
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                String.class,
                tableName,
                columnName);
        return "NO".equalsIgnoreCase(nullable);
    }
}
