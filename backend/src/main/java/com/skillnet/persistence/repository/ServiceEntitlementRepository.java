package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.ServiceEntitlement;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceEntitlementRepository extends JpaRepository<ServiceEntitlement, Long> {

    List<ServiceEntitlement> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<ServiceEntitlement> findByPayment_Id(Long paymentId);

    boolean existsByUser_IdAndStatusAndUsesRemainingGreaterThanAndOffering_CapabilityKey(
            Long userId, String status, int usesRemaining, String capabilityKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT e FROM ServiceEntitlement e
            JOIN FETCH e.offering o
            WHERE e.user.id = :userId
              AND e.status = 'ACTIVE'
              AND e.usesRemaining > 0
              AND o.capabilityKey = :capabilityKey
            ORDER BY e.id ASC
            """)
    Optional<ServiceEntitlement> findFirstActiveForCapability(
            @Param("userId") Long userId, @Param("capabilityKey") String capabilityKey);

    @Query(
            """
            SELECT COUNT(e) > 0 FROM ServiceEntitlement e
            JOIN e.offering o
            WHERE e.user.id = :userId AND o.capabilityKey = :capabilityKey
            """)
    boolean existsByUserAndCapabilityKey(@Param("userId") Long userId, @Param("capabilityKey") String capabilityKey);
}
