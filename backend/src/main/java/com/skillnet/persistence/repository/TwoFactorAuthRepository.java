package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.TwoFactorAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {

    Optional<TwoFactorAuth> findByUser_Id(Long userId);

    boolean existsByUser_IdAndEnabledTrue(Long userId);
}
