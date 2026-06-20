package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.CourseReview;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id IN :courseIds")
    Double findAverageRatingByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
