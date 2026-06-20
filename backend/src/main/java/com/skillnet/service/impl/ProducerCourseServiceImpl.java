package com.skillnet.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.domain.AuditAction;
import com.skillnet.domain.CourseFormat;
import com.skillnet.domain.CourseStatus;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.core.Coupon;
import com.skillnet.persistence.repository.CouponRepository;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.ProducerCourseService;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.service.media.StoredMedia;
import com.skillnet.service.notification.NotificationPublisher;
import com.skillnet.web.dto.request.CreateCourseCouponRequestDTO;
import com.skillnet.web.dto.request.CreateCourseRequestDTO;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.UpdateCourseBasicsRequestDTO;
import com.skillnet.web.dto.request.UpdateCourseMessagesRequestDTO;
import com.skillnet.web.dto.request.UpdateCoursePricingRequestDTO;
import com.skillnet.web.dto.response.CourseBasicsResponseDTO;
import com.skillnet.web.dto.response.CourseCouponResponseDTO;
import com.skillnet.web.dto.response.CourseMediaUploadResponseDTO;
import com.skillnet.web.dto.response.CourseMessagesResponseDTO;
import com.skillnet.web.dto.response.CoursePricingResponseDTO;
import com.skillnet.web.dto.response.ProducerCourseSummaryDTO;
import java.io.IOException;
import java.io.InputStream;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProducerCourseServiceImpl implements ProducerCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MediaStorageService mediaStorageService;
    private final AuditService auditService;
    private final NotificationPublisher notificationPublisher;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public ProducerCourseServiceImpl(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CouponRepository couponRepository,
            EnrollmentRepository enrollmentRepository,
            MediaStorageService mediaStorageService,
            AuditService auditService,
            NotificationPublisher notificationPublisher) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.mediaStorageService = mediaStorageService;
        this.auditService = auditService;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    @Transactional
    public ProducerCourseSummaryDTO createDraftCourse(Long professorId, CreateCourseRequestDTO dto) {
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found"));

        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        if (title.isBlank()) {
            title = "Nuevo curso";
        }

        Course course = new Course();
        course.setProfessor(professor);
        course.setTitle(title);
        course.setFormat(CourseFormat.fromDbValue(dto.getCourseFormat()).getDbValue());
        course.setCategory(dto.getCategory());
        course.setSubcategory(dto.getSubcategory());
        course.setWhatYouWillLearn(dto.getWhatYouWillLearn());
        course.setTargetAudience(dto.getTargetAudience());
        course.setStatus(CourseStatus.DRAFT.getDbValue());
        course.setCreatedAt(Instant.now());
        course.setSlug(CourseSlugUtils.uniqueSlug(courseRepository, title, course.getFormat(), null));
        applyDefaults(course);

        Course saved = courseRepository.save(course);
        auditService.logAction(
                AuditAction.CREATE_COURSE,
                AuditAction.ENTITY_COURSE,
                saved.getId(),
                professor.getEmail(),
                "Borrador creado: " + saved.getTitle());
        return toSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProducerCourseSummaryDTO> getMyCourses(Long professorId) {
        return courseRepository.findAllByProfessorIdOrderByCreatedAtDesc(professorId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public ProducerCourseSummaryDTO publishCourse(Long courseId, Long professorId) {
        Course course = requireCourseOwned(courseId, professorId);
        course.setStatus(CourseStatus.PUBLISHED.getDbValue());
        Course saved = courseRepository.save(course);
        auditService.logAction(
                AuditAction.PUBLISH_COURSE,
                AuditAction.ENTITY_COURSE,
                saved.getId(),
                saved.getProfessor().getEmail(),
                "Curso publicado: " + saved.getTitle());
        notificationPublisher.publish(
                saved.getProfessor(),
                "course_published",
                "Curso publicado",
                "\"" + saved.getTitle() + "\" ya está visible en el marketplace.",
                frontendBaseUrl + "/courses");
        return toSummary(saved);
    }

    @Override
    @Transactional
    public ProducerCourseSummaryDTO unpublishCourse(Long courseId, Long professorId) {
        Course course = requireCourseOwned(courseId, professorId);
        course.setStatus(CourseStatus.DRAFT.getDbValue());
        Course saved = courseRepository.save(course);
        auditService.logAction(
                AuditAction.SET_DRAFT_COURSE,
                AuditAction.ENTITY_COURSE,
                saved.getId(),
                saved.getProfessor().getEmail(),
                "Curso pasado a borrador: " + saved.getTitle());
        return toSummary(saved);
    }

    @Override
    @Transactional
    public CourseBasicsResponseDTO updateBasics(
            Long courseId, Long professorId, UpdateCourseBasicsRequestDTO dto) {
        Course course = requireCourseOwned(courseId, professorId);
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            course.setTitle(dto.getTitle().trim());
            course.setSlug(
                    CourseSlugUtils.uniqueSlug(
                            courseRepository, dto.getTitle().trim(), course.getFormat(), course.getId()));
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            course.setPrice(dto.getPrice());
            course.setOriginalPrice(dto.getPrice());
        }
        if (dto.getImageUrl() != null) {
            course.setImageUrl(dto.getImageUrl().trim());
        }
        if (dto.getVideoUrl() != null) {
            course.setVideoUrl(dto.getVideoUrl().trim());
        }
        if (dto.getLanguage() != null && !dto.getLanguage().isBlank()) {
            course.setLanguage(dto.getLanguage().trim());
        }
        if (dto.getLevel() != null && !dto.getLevel().isBlank()) {
            course.setLevel(dto.getLevel().trim());
        }
        if (dto.getCategory() != null) {
            course.setCategory(dto.getCategory().trim());
        }
        if (dto.getSubcategory() != null) {
            course.setSubcategory(dto.getSubcategory().trim());
        }
        if (dto.getWhatYouWillLearn() != null) {
            course.setWhatYouWillLearn(dto.getWhatYouWillLearn());
        }
        if (dto.getTargetAudience() != null) {
            course.setTargetAudience(dto.getTargetAudience());
        }
        Course saved = courseRepository.save(course);
        auditService.logAction(
                AuditAction.UPDATE_COURSE,
                AuditAction.ENTITY_COURSE,
                saved.getId(),
                saved.getProfessor().getEmail(),
                "Datos básicos actualizados: " + saved.getTitle());
        return toBasicsResponse(saved);
    }

    @Override
    @Transactional
    public CourseMediaUploadResponseDTO uploadCourseMedia(
            Long courseId,
            Long professorId,
            String kind,
            String originalFilename,
            String contentType,
            InputStream input,
            long size)
            throws IOException {
        Course course = requireCourseOwned(courseId, professorId);
        String normalizedKind = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);

        StoredMedia stored;
        switch (normalizedKind) {
            case "cover" -> {
                mediaStorageService.validateCover(contentType, size);
                stored = mediaStorageService.storeCourseFile(
                        courseId, "cover", originalFilename, contentType, input, size);
                course.setImageFile(stored.storageKey());
                course.setImageUrl(stored.publicUrl());
            }
            case "promo_video" -> {
                mediaStorageService.validatePromoVideo(contentType, size);
                stored = mediaStorageService.storeCourseFile(
                        courseId, "promo-video", originalFilename, contentType, input, size);
                course.setVideoFile(stored.storageKey());
                course.setVideoUrl(stored.publicUrl());
            }
            case "resource" -> {
                mediaStorageService.validateResourceFile(contentType, size);
                stored = mediaStorageService.storeCourseFile(
                        courseId, "resources", originalFilename, contentType, input, size);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "kind debe ser cover, promo_video o resource");
        }

        courseRepository.save(course);

        CourseMediaUploadResponseDTO dto = new CourseMediaUploadResponseDTO();
        dto.setKind(normalizedKind);
        dto.setStorageKey(stored.storageKey());
        dto.setPublicUrl(stored.publicUrl());
        dto.setImageUrl(course.getImageUrl());
        dto.setVideoUrl(course.getVideoUrl());
        return dto;
    }

    @Override
    @Transactional
    public CoursePricingResponseDTO updatePricing(
            Long courseId, Long professorId, UpdateCoursePricingRequestDTO dto) {
        Course course = requireCourseOwned(courseId, professorId);

        if (dto.getCurrency() != null && !dto.getCurrency().isBlank()) {
            course.setCurrency(dto.getCurrency().trim().toUpperCase(Locale.ROOT));
        }

        BigDecimal basePrice = dto.getPrice();
        if (basePrice != null) {
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El precio base debe ser mayor a cero.");
            }
            if (Boolean.TRUE.equals(dto.getOnSale()) && dto.getDiscountPrice() != null) {
                if (dto.getDiscountPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "El precio promocional debe ser mayor a cero.");
                }
                course.setOriginalPrice(basePrice);
                course.setPrice(dto.getDiscountPrice());
                course.setOnSale(true);
            } else {
                course.setPrice(basePrice);
                course.setOriginalPrice(basePrice);
                course.setOnSale(false);
            }
        }

        if (dto.getAffiliateCommission() != null) {
            BigDecimal commission = dto.getAffiliateCommission();
            if (commission.compareTo(BigDecimal.ZERO) < 0
                    || commission.compareTo(new BigDecimal("100")) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "La comisión de afiliado debe estar entre 0 y 100.");
            }
            course.setAffiliateCommission(commission);
        }

        if (dto.getAffiliationType() != null && !dto.getAffiliationType().isBlank()) {
            course.setAffiliatePolicy(dto.getAffiliationType().trim().toLowerCase(Locale.ROOT));
        }

        courseRepository.save(course);
        return toPricingResponse(course);
    }

    @Override
    @Transactional
    public CourseMessagesResponseDTO updateMessages(
            Long courseId, Long professorId, UpdateCourseMessagesRequestDTO dto) {
        Course course = requireCourseOwned(courseId, professorId);
        if (dto.getWelcomeMessage() != null) {
            course.setWelcomeMessage(dto.getWelcomeMessage());
        }
        if (dto.getCongratulationsMessage() != null) {
            course.setCongratulationsMessage(dto.getCongratulationsMessage());
        }
        courseRepository.save(course);
        return toMessagesResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCouponResponseDTO> listCoupons(Long courseId, Long professorId) {
        requireCourseOwned(courseId, professorId);
        return couponRepository.findByApplicableCourse_IdOrderByIdDesc(courseId).stream()
                .map(this::toCouponResponse)
                .toList();
    }

    @Override
    @Transactional
    public CourseCouponResponseDTO createCoupon(
            Long courseId, Long professorId, CreateCourseCouponRequestDTO dto) {
        Course course = requireCourseOwned(courseId, professorId);
        String code = dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cupón con ese código.");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setPercentOff(dto.getPercentOff());
        coupon.setAmountOff(
                dto.getAmountOff() != null ? dto.getAmountOff() : BigDecimal.ZERO);
        coupon.setApplicableCourse(course);
        coupon.setActive(true);
        coupon.setTimesRedeemed(0);
        couponRepository.save(coupon);
        return toCouponResponse(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long courseId, Long professorId, Long couponId) {
        requireCourseOwned(courseId, professorId);
        Coupon coupon = couponRepository
                .findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found with id: " + couponId));
        if (coupon.getApplicableCourse() == null
                || !coupon.getApplicableCourse().getId().equals(courseId)) {
            throw new AccessDeniedException("Este cupón no pertenece al curso indicado.");
        }
        couponRepository.delete(coupon);
    }

    private CoursePricingResponseDTO toPricingResponse(Course course) {
        CoursePricingResponseDTO dto = new CoursePricingResponseDTO();
        dto.setId(course.getId());
        dto.setCurrency(course.getCurrency());
        dto.setPrice(course.getPrice());
        dto.setOriginalPrice(course.getOriginalPrice());
        dto.setOnSale(course.isOnSale());
        dto.setDiscountPrice(course.isOnSale() ? course.getPrice() : null);
        dto.setAffiliateCommission(course.getAffiliateCommission());
        dto.setAffiliationType(course.getAffiliatePolicy());
        return dto;
    }

    private CourseMessagesResponseDTO toMessagesResponse(Course course) {
        CourseMessagesResponseDTO dto = new CourseMessagesResponseDTO();
        dto.setId(course.getId());
        dto.setWelcomeMessage(course.getWelcomeMessage());
        dto.setCongratulationsMessage(course.getCongratulationsMessage());
        return dto;
    }

    private CourseCouponResponseDTO toCouponResponse(Coupon coupon) {
        CourseCouponResponseDTO dto = new CourseCouponResponseDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setPercentOff(coupon.getPercentOff());
        dto.setAmountOff(coupon.getAmountOff());
        dto.setActive(coupon.isActive());
        dto.setValidTo(coupon.getValidTo());
        return dto;
    }

    private Course requireCourseOwned(Long courseId, Long professorId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        if (course.getProfessor() == null || !course.getProfessor().getId().equals(professorId)) {
            throw new AccessDeniedException(
                    "Este curso no pertenece a tu cuenta de infoproductor.");
        }
        return course;
    }

    private CourseBasicsResponseDTO toBasicsResponse(Course course) {
        CourseBasicsResponseDTO dto = new CourseBasicsResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setPrice(course.getPrice());
        dto.setImageUrl(course.getImageUrl());
        dto.setVideoUrl(course.getVideoUrl());
        dto.setLanguage(course.getLanguage());
        dto.setLevel(course.getLevel());
        dto.setCategory(course.getCategory());
        dto.setSubcategory(course.getSubcategory());
        dto.setStatus(course.getStatus());
        return dto;
    }

    private void applyDefaults(Course course) {
        course.setSoftware(JsonNodeFactory.instance.arrayNode());
        course.setOriginalPrice(BigDecimal.ZERO);
        course.setPrice(BigDecimal.ZERO);
        course.setAffiliateCommission(BigDecimal.ZERO);
        course.setDurationHours(0);
        course.setDurationMinutes(0);
    }

    private ProducerCourseSummaryDTO toSummary(Course course) {
        ProducerCourseSummaryDTO dto = new ProducerCourseSummaryDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setSlug(course.getSlug());
        dto.setCourseFormat(course.getCourseFormat());
        dto.setStatus(course.getStatus());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setImageUrl(
                mediaStorageService.resolveCourseImageUrl(course.getImageUrl(), course.getImageFile()));
        dto.setEnrollmentCount(enrollmentRepository.countByCourse_Id(course.getId()));
        return dto;
    }

    private String generateUniqueSlug(String title, String courseFormat) {
        return CourseSlugUtils.uniqueSlug(courseRepository, title, courseFormat, null);
    }
}
