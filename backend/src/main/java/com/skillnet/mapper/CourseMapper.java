package com.skillnet.mapper;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.web.dto.request.CourseRequestDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import com.skillnet.web.dto.response.CourseSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;

    public CourseMapper(
            UserMapper userMapper,
            UserRepository userRepository,
            MediaStorageService mediaStorageService) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.mediaStorageService = mediaStorageService;
    }

    public CourseResponseDTO toResponseDTO(Course course) {
        if (course == null) {
            return null;
        }
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setWhatYouWillLearn(course.getWhatYouWillLearn());
        dto.setTargetAudience(course.getTargetAudience());
        dto.setDurationHours(course.getDurationHours());
        dto.setDurationMinutes(course.getDurationMinutes());
        dto.setRequirements(course.getRequirements());
        dto.setWelcomeMessage(course.getWelcomeMessage());
        dto.setCongratulationsMessage(course.getCongratulationsMessage());
        dto.setCategory(course.getCategory());
        dto.setSubcategory(course.getSubcategory());
        dto.setLevel(course.getLevel());
        dto.setLanguage(course.getLanguage());
        dto.setCourseFormat(course.getCourseFormat());
        dto.setSoftware(course.getSoftware());
        dto.setHasSubtitles(course.isHasSubtitles());
        dto.setFlexibleSchedule(course.isFlexibleSchedule());
        dto.setHasPracticalExperience(course.isHasPracticalExperience());
        dto.setOriginalPrice(course.getOriginalPrice());
        dto.setPrice(course.getPrice());
        dto.setCurrency(course.getCurrency());
        dto.setOnSale(course.isOnSale());
        dto.setTaxIncluded(course.isTaxIncluded());
        User professor = course.getProfessor();
        if (professor != null) {
            dto.setProfessorId(professor.getId());
            dto.setProfessor(userMapper.toProfessorSummaryDTO(professor));
        }
        dto.setStatus(course.getStatus());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setSlug(course.getSlug());
        dto.setImageUrl(mediaStorageService.resolveCourseImageUrl(course.getImageUrl(), course.getImageFile()));
        dto.setImageFile(course.getImageFile());
        dto.setVideoUrl(mediaStorageService.resolveCourseVideoUrl(course.getVideoUrl(), course.getVideoFile()));
        dto.setVideoFile(course.getVideoFile());
        dto.setAffiliateCommission(course.getAffiliateCommission());
        dto.setAffiliatePolicy(course.getAffiliatePolicy());
        dto.setAlly(course.getAlly());
        dto.setSecurityStatus(course.getSecurityStatus());
        dto.setSecurityScanReport(course.getSecurityScanReport());
        dto.setLastScannedAt(course.getLastScannedAt());
        return dto;
    }

    public CourseSummaryDTO toSummaryDTO(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseSummaryDTO(
                course.getId(),
                course.getTitle(),
                course.getSlug(),
                course.getStatus());
    }

    public Course toEntity(CourseRequestDTO dto) {
        Course course = new Course();
        applyToEntity(course, dto);
        return course;
    }

    public void applyToEntity(Course course, CourseRequestDTO dto) {
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setWhatYouWillLearn(dto.getWhatYouWillLearn());
        course.setTargetAudience(dto.getTargetAudience());
        course.setDurationHours(dto.getDurationHours());
        course.setDurationMinutes(dto.getDurationMinutes());
        course.setRequirements(dto.getRequirements());
        course.setCategory(dto.getCategory());
        course.setSubcategory(dto.getSubcategory());
        course.setLevel(dto.getLevel());
        course.setLanguage(dto.getLanguage());
        course.setCourseFormat(dto.getCourseFormat());
        course.setSoftware(dto.getSoftware());
        course.setHasSubtitles(dto.isHasSubtitles());
        course.setFlexibleSchedule(dto.isFlexibleSchedule());
        course.setHasPracticalExperience(dto.isHasPracticalExperience());
        course.setOriginalPrice(dto.getOriginalPrice());
        course.setPrice(dto.getPrice());
        course.setCurrency(dto.getCurrency());
        course.setOnSale(dto.isOnSale());
        course.setTaxIncluded(dto.isTaxIncluded());
        if (dto.getProfessorId() != null) {
            course.setProfessor(resolveProfessor(dto.getProfessorId()));
        }
        course.setStatus(dto.getStatus());
        course.setCreatedAt(dto.getCreatedAt());
        course.setSlug(dto.getSlug());
        course.setImageUrl(dto.getImageUrl());
        course.setImageFile(dto.getImageFile());
        course.setVideoUrl(dto.getVideoUrl());
        course.setVideoFile(dto.getVideoFile());
        course.setAffiliateCommission(dto.getAffiliateCommission());
        course.setAffiliatePolicy(dto.getAffiliatePolicy());
        course.setAlly(dto.getAlly());
        course.setSecurityStatus(dto.getSecurityStatus());
        course.setSecurityScanReport(dto.getSecurityScanReport());
        course.setLastScannedAt(dto.getLastScannedAt());
    }

    private User resolveProfessor(Long professorId) {
        if (professorId == null) {
            return null;
        }
        return userRepository.findById(professorId).orElse(null);
    }
}
