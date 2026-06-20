package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(
            value =
                    """
                    SELECT a.*
                    FROM core_auditlog a
                    LEFT JOIN core_user u ON u.id = a.user_id
                    WHERE (:email IS NULL
                        OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :email, '%'))
                        OR LOWER(COALESCE(a.changes ->> 'userEmail', '')) LIKE LOWER(CONCAT('%', :email, '%')))
                      AND (:action IS NULL OR a.action = :action)
                      AND (CAST(:startDate AS timestamptz) IS NULL OR a.timestamp >= CAST(:startDate AS timestamptz))
                      AND (CAST(:endDate AS timestamptz) IS NULL OR a.timestamp <= CAST(:endDate AS timestamptz))
                    ORDER BY a.timestamp DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM core_auditlog a
                    LEFT JOIN core_user u ON u.id = a.user_id
                    WHERE (:email IS NULL
                        OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :email, '%'))
                        OR LOWER(COALESCE(a.changes ->> 'userEmail', '')) LIKE LOWER(CONCAT('%', :email, '%')))
                      AND (:action IS NULL OR a.action = :action)
                      AND (CAST(:startDate AS timestamptz) IS NULL OR a.timestamp >= CAST(:startDate AS timestamptz))
                      AND (CAST(:endDate AS timestamptz) IS NULL OR a.timestamp <= CAST(:endDate AS timestamptz))
                    """,
            nativeQuery = true)
    Page<AuditLog> findFiltered(
            @Param("email") String email,
            @Param("action") String action,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT a.*
                    FROM core_auditlog a
                    LEFT JOIN core_user u ON u.id = a.user_id
                    WHERE (:email IS NULL
                        OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :email, '%'))
                        OR LOWER(COALESCE(a.changes ->> 'userEmail', '')) LIKE LOWER(CONCAT('%', :email, '%')))
                      AND (:action IS NULL OR a.action = :action)
                      AND (CAST(:startDate AS timestamptz) IS NULL OR a.timestamp >= CAST(:startDate AS timestamptz))
                      AND (CAST(:endDate AS timestamptz) IS NULL OR a.timestamp <= CAST(:endDate AS timestamptz))
                    ORDER BY a.timestamp DESC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    java.util.List<AuditLog> findFilteredForExport(
            @Param("email") String email,
            @Param("action") String action,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);
}
