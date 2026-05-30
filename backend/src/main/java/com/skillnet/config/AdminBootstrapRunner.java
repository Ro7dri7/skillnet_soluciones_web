package com.skillnet.config;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crea o repara el superadmin local si hace falta (desarrollo / primer arranque). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "admin@skillnet.com";
    public static final String ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User admin =
                userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseGet(User::new);
        boolean created = admin.getId() == null;

        admin.setUsername("superadmin");
        admin.setEmail(ADMIN_EMAIL);
        if (created) {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        }
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        admin.setSuperUser(true);
        admin.setStaff(true);
        admin.setActive(true);
        admin.setRole("admin");
        admin.setActiveRole("admin");
        admin.setStudent(true);
        admin.setInfoproductor(true);
        admin.setAffiliate(false);
        if (admin.getDateJoined() == null) {
            admin.setDateJoined(Instant.now());
        }
        if (admin.getSpecialties() == null) {
            admin.setSpecialties(JsonNodeFactory.instance.objectNode());
        }
        if (admin.getSocialLinks() == null) {
            admin.setSocialLinks(JsonNodeFactory.instance.objectNode());
        }

        User saved = userRepository.save(admin);
        if (created) {
            log.info(
                    "Superadmin persistido en core_user (id={}): {} / {}",
                    saved.getId(),
                    ADMIN_EMAIL,
                    ADMIN_PASSWORD);
        } else {
            log.info("Superadmin verificado en core_user (id={}): {}", saved.getId(), ADMIN_EMAIL);
        }
    }
}
