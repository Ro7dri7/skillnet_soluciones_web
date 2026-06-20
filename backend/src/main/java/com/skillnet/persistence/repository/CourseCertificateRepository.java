package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.CourseCertificate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCertificateRepository extends JpaRepository<CourseCertificate, Long> {

    List<CourseCertificate> findByCourse_IdAndActiveTrue(Long courseId);

    List<CourseCertificate> findByStudent_IdAndActiveTrue(Long studentId);

    Optional<CourseCertificate> findFirstByCourse_IdAndStudent_IdAndActiveTrue(Long courseId, Long studentId);

    long countByStudent_IdAndActiveTrue(Long studentId);
}
