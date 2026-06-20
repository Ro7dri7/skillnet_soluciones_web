package com.skillnet.service.admin;

import com.skillnet.web.dto.response.DbSchemaHealthResponseDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbSchemaHealthService {

    private static final List<String> EXPECTED_TABLES = List.of(
            "core_coupon",
            "core_notification",
            "core_coursecertificate",
            "core_passwordresettoken",
            "core_enrollment",
            "core_progress",
            "payments_payment",
            "core_gammageneration",
            "core_podcastgeneration",
            "core_serviceentitlement",
            "core_infoproductorserviceoffering",
            "core_auditlog");

    private final JdbcTemplate jdbcTemplate;

    public DbSchemaHealthResponseDTO inspect() {
        Map<String, Boolean> tablesPresent = new LinkedHashMap<>();
        Map<String, Long> rowCounts = new LinkedHashMap<>();
        Map<String, String> notes = new LinkedHashMap<>();

        for (String table : EXPECTED_TABLES) {
            boolean exists = tableExists(table);
            tablesPresent.put(table, exists);
            if (exists) {
                rowCounts.put(table, countRows(table));
            } else {
                rowCounts.put(table, -1L);
                notes.put(table, "Tabla ausente — reinicia el backend con ddl-auto=update o revisa migraciones");
            }
        }

        if (tableExists("payments_payment")) {
            rowCounts.put(
                    "payments_payment_with_coupon",
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM payments_payment WHERE coupon_id IS NOT NULL", Long.class));
        }

        if (tableExists("core_coursecertificate")) {
            rowCounts.put(
                    "core_coursecertificate_auto",
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM core_coursecertificate WHERE certificate_file LIKE 'auto:%'",
                            Long.class));
        }

        notes.put(
                "hint",
                "Tras checkout/cupón revisa payments_payment.coupon_id; tras completar curso core_coursecertificate; tras eventos core_notification");

        return DbSchemaHealthResponseDTO.builder()
                .tablesPresent(tablesPresent)
                .rowCounts(rowCounts)
                .notes(notes)
                .build();
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

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }
}
