package com.skillnet.service.review;

import com.skillnet.domain.CourseStatus;
import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.CourseReview;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.CourseReviewRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.CreateCourseReviewRequestDTO;
import com.skillnet.web.dto.response.CourseReviewResponseDTO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<CourseReviewResponseDTO> listByCourseSlug(String slug) {
        Course course = CourseSlugUtils.resolveCourse(courseRepository, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!CourseStatus.PUBLISHED.getDbValue().equals(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
        }

        return courseReviewRepository.findByCourse_IdOrderByCreatedAtDesc(course.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourseReviewResponseDTO createReview(String slug, Long userId, CreateCourseReviewRequestDTO dto) {
        Course course = CourseSlugUtils.resolveCourse(courseRepository, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!CourseStatus.PUBLISHED.getDbValue().equals(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso no está publicado");
        }

        if (!enrollmentRepository.existsByUser_IdAndCourse_Id(userId, course.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes estar inscrito para dejar una reseña");
        }

        if (courseReviewRepository.existsByCourse_IdAndUser_Id(course.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has dejado una reseña para este curso");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Instant now = Instant.now();

        CourseReview review = new CourseReview();
        review.setCourse(course);
        review.setUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setHelpfulCount(0);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        return toResponse(courseReviewRepository.save(review));
    }

    private CourseReviewResponseDTO toResponse(CourseReview review) {
        return CourseReviewResponseDTO.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .userId(review.getUser().getId())
                .user(userMapper.toSummaryDTO(review.getUser()))
                .rating(review.getRating())
                .comment(review.getComment())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
