package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByRole(String role);

    List<User> findByActive(boolean active);

    long countByDateJoinedGreaterThanEqualAndDateJoinedLessThan(Instant start, Instant end);

    long countByActiveFalse();

    long countByDateJoinedGreaterThanEqualAndDateJoinedLessThanAndActiveFalse(Instant start, Instant end);
}
