package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Enrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    List<Enrollment> findByUser_Id(Long userId);

    List<Enrollment> findByCourse_Id(Long courseId);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    long countByCourse_Id(Long courseId);
}
