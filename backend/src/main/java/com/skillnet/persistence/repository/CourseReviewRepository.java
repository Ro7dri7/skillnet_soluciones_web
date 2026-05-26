package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.CourseReview;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id IN :courseIds")
    Double findAverageRatingByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
