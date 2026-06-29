package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.EmailVerificationToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findFirstByUser_IdAndRegistrationCodeAndUsedFalseAndExpiresAtAfter(
            Long userId, String registrationCode, Instant now);

    List<EmailVerificationToken> findByUser_Id(Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void markAllUsedForUser(@Param("userId") Long userId);
}
