package com.skillnet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Section;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.SectionRepository;
import com.skillnet.service.CurriculumService;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.util.LessonContentNormalizer;
import com.skillnet.web.dto.request.CreateCurriculumLessonRequestDTO;
import com.skillnet.web.dto.request.CreateSectionRequestDTO;
import com.skillnet.web.dto.request.LessonUpdateRequestDTO;
import com.skillnet.web.dto.request.SectionUpdateRequestDTO;
import com.skillnet.web.dto.response.CurriculumLessonResponseDTO;
import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class CurriculumServiceImpl implements CurriculumService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    private final MediaStorageService mediaStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    public CurriculumServiceImpl(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            EnrollmentRepository enrollmentRepository,
            ObjectMapper objectMapper,
            MediaStorageService mediaStorageService) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.objectMapper = objectMapper;
        this.mediaStorageService = mediaStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumModuleResponseDTO> getCurriculum(Long courseId, Long professorId) {
        Course course = requireCourseOwned(courseId, professorId);
        return buildCurriculumModules(course.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumModuleResponseDTO> getCurriculumForLearner(Long courseId, Long userId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        if (!canAccessCurriculum(course, userId)) {
            throw new AccessDeniedException("No tienes acceso al temario de este curso");
        }
        return buildCurriculumModules(course.getId());
    }

    private List<CurriculumModuleResponseDTO> buildCurriculumModules(Long courseId) {
        List<Section> sections = sectionRepository.findByCourse_IdOrderByOrderIndexAsc(courseId);
        List<CurriculumModuleResponseDTO> result = new ArrayList<>();

        for (Section section : sections) {
            CurriculumModuleResponseDTO moduleDto = new CurriculumModuleResponseDTO();
            moduleDto.setId(section.getId());
            moduleDto.setTitle(section.getTitle());
            moduleDto.setOrderIndex(section.getOrderIndex());

            List<Lesson> lessons = lessonRepository.findBySection_IdOrderByOrderIndexAsc(section.getId());
            for (Lesson lesson : lessons) {
                moduleDto.getLessons().add(toLessonResponse(lesson));
            }
            result.add(moduleDto);
        }
        return result;
    }

    private boolean canAccessCurriculum(Course course, Long userId) {
        User professor = course.getProfessor();
        if (professor != null && professor.getId().equals(userId)) {
            return true;
        }
        return enrollmentRepository.existsByUser_IdAndCourse_Id(userId, course.getId());
    }

    @Override
    @Transactional
    public CurriculumModuleResponseDTO createSection(
            Long courseId, Long professorId, CreateSectionRequestDTO request) {
        Course course = requireCourseOwned(courseId, professorId);

        Section section = new Section();
        section.setCourse(course);
        section.setTitle(request.getTitle().trim());
        section.setOrderIndex(request.getOrderIndex());

        Section saved = sectionRepository.save(section);

        CurriculumModuleResponseDTO dto = new CurriculumModuleResponseDTO();
        dto.setId(saved.getId());
        dto.setTitle(saved.getTitle());
        dto.setOrderIndex(saved.getOrderIndex());
        return dto;
    }

    @Override
    @Transactional
    public CurriculumLessonResponseDTO createLesson(
            Long sectionId, Long professorId, CreateCurriculumLessonRequestDTO request) {
        Section section = sectionRepository
                .findByIdWithCourse(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + sectionId));
        Course course = requireCourseOwned(section.getCourse().getId(), professorId);

        String uiContentType = normalizeUiContentType(request.getContentType());
        int orderIndex = (int) lessonRepository.findBySection_Id(sectionId).stream().count();

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setSection(section);
        lesson.setTitle(request.getTitle().trim());
        if (request.getBlocks() != null && request.getBlocks().isArray()) {
            JsonNode normalizedBlocks = LessonContentNormalizer.normalizeBlocks(request.getBlocks());
            String primaryType = primaryTypeFromBlocks(normalizedBlocks);
            lesson.setContent(buildLessonContentFromBlocks(normalizedBlocks, primaryType));
            lesson.setContentType("quiz".equals(primaryType) ? "quiz" : "lesson");
        } else {
            lesson.setContent(buildLessonContent(uiContentType, request.getTextContent(), request.getQuizData()));
            lesson.setContentType("quiz".equals(uiContentType) ? "quiz" : "lesson");
        }
        lesson.setResourceUrl(request.getResourceUrl() != null ? request.getResourceUrl().trim() : null);
        lesson.setOrderIndex(orderIndex);
        lesson.setStatus("draft");
        lesson.setVersion(1);
        lesson.setUpdatedAt(Instant.now());
        lesson.setSecurityStatus("pending");
        lesson.setSecurityScanReport(JsonNodeFactory.instance.objectNode());

        Lesson saved = lessonRepository.save(lesson);
        return toLessonResponse(saved);
    }

    @Override
    @Transactional
    public CurriculumLessonResponseDTO updateLesson(
            Long lessonId, Long professorId, CreateCurriculumLessonRequestDTO request) {
        Lesson lesson = lessonRepository
                .findByIdWithCourse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id: " + lessonId));
        requireCourseOwned(lesson.getCourse().getId(), professorId);

        String uiContentType = normalizeUiContentType(request.getContentType());
        lesson.setTitle(request.getTitle().trim());
        lesson.setContent(buildLessonContent(uiContentType, request.getTextContent(), request.getQuizData()));
        lesson.setResourceUrl(request.getResourceUrl() != null ? request.getResourceUrl().trim() : null);
        lesson.setContentType("quiz".equals(uiContentType) ? "quiz" : "lesson");
        lesson.setUpdatedAt(Instant.now());
        lesson.setVersion(lesson.getVersion() + 1);

        Lesson saved = lessonRepository.save(lesson);
        return toLessonResponse(saved);
    }

    @Override
    @Transactional
    public CurriculumLessonResponseDTO patchLesson(
            Long lessonId, Long professorId, LessonUpdateRequestDTO request) {
        Lesson lesson = lessonRepository
                .findByIdWithCourse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id: " + lessonId));
        requireCourseOwned(lesson.getCourse().getId(), professorId);

        LessonContentPayload current = parseLessonContent(lesson);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            lesson.setTitle(request.getTitle().trim());
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }
        if (request.getResourceUrl() != null) {
            lesson.setResourceUrl(request.getResourceUrl().trim());
        }

        if (request.getBlocks() != null && request.getBlocks().isArray()) {
            JsonNode normalizedBlocks = LessonContentNormalizer.normalizeBlocks(request.getBlocks());
            String primaryType = primaryTypeFromBlocks(normalizedBlocks);
            lesson.setContent(buildLessonContentFromBlocks(normalizedBlocks, primaryType));
            lesson.setContentType("quiz".equals(primaryType) ? "quiz" : "lesson");
        } else {
            String uiContentType = request.getContentType() != null && !request.getContentType().isBlank()
                    ? normalizeUiContentType(request.getContentType())
                    : current.uiContentType();
            String textContent =
                    request.getTextContent() != null ? request.getTextContent() : current.textContent();
            JsonNode quizData = request.getQuizData() != null ? request.getQuizData() : current.quizData();

            if (textContent != null && textContent.isBlank()) {
                textContent = "";
            }
            lesson.setContent(buildLessonContent(uiContentType, textContent, quizData));
            lesson.setContentType("quiz".equals(uiContentType) ? "quiz" : "lesson");
        }
        lesson.setUpdatedAt(Instant.now());
        lesson.setVersion(lesson.getVersion() + 1);

        Lesson saved = lessonRepository.save(lesson);
        return toLessonResponse(saved);
    }

    @Override
    @Transactional
    public CurriculumModuleResponseDTO updateSection(
            Long sectionId, Long professorId, SectionUpdateRequestDTO request) {
        Section section = sectionRepository
                .findByIdWithCourse(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + sectionId));
        requireCourseOwned(section.getCourse().getId(), professorId);

        if (request.getTitle() == null && request.getOrderIndex() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            section.setTitle(request.getTitle().trim());
        }
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }

        Section saved = sectionRepository.save(section);

        CurriculumModuleResponseDTO dto = new CurriculumModuleResponseDTO();
        dto.setId(saved.getId());
        dto.setTitle(saved.getTitle());
        dto.setOrderIndex(saved.getOrderIndex());
        for (Lesson lesson : lessonRepository.findBySection_IdOrderByOrderIndexAsc(saved.getId())) {
            dto.getLessons().add(toLessonResponse(lesson));
        }
        return dto;
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId, Long professorId) {
        Section section = sectionRepository
                .findByIdWithCourse(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + sectionId));
        requireCourseOwned(section.getCourse().getId(), professorId);

        List<Lesson> lessons = lessonRepository.findBySection_Id(sectionId);
        for (Lesson lesson : lessons) {
            purgeLessonReferences(lesson.getId());
        }
        lessonRepository.deleteAll(lessons);
        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId, Long professorId) {
        Lesson lesson = lessonRepository
                .findByIdWithCourse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id: " + lessonId));
        requireCourseOwned(lesson.getCourse().getId(), professorId);
        purgeLessonReferences(lessonId);
        lessonRepository.delete(lesson);
    }

    /** Elimina filas hijas que referencian la lección (evita violación FK al borrar). */
    private void purgeLessonReferences(Long lessonId) {
        entityManager
                .createNativeQuery(
                        "DELETE FROM core_lessoncomment WHERE lesson_id = :lessonId AND parent_id IS NOT NULL")
                .setParameter("lessonId", lessonId)
                .executeUpdate();
        entityManager
                .createNativeQuery("DELETE FROM core_lessoncomment WHERE lesson_id = :lessonId")
                .setParameter("lessonId", lessonId)
                .executeUpdate();
        entityManager
                .createNativeQuery("DELETE FROM core_progress WHERE lesson_id = :lessonId")
                .setParameter("lessonId", lessonId)
                .executeUpdate();
        entityManager
                .createNativeQuery("DELETE FROM core_studentnote WHERE lesson_id = :lessonId")
                .setParameter("lessonId", lessonId)
                .executeUpdate();
        entityManager
                .createNativeQuery("DELETE FROM core_assetuploadsession WHERE lesson_id = :lessonId")
                .setParameter("lessonId", lessonId)
                .executeUpdate();
    }

    private record LessonContentPayload(
            String uiContentType, String textContent, JsonNode quizData, JsonNode blocks) {}

    private LessonContentPayload parseLessonContent(Lesson lesson) {
        String uiType = "text";
        if ("quiz".equalsIgnoreCase(lesson.getContentType())) {
            uiType = "quiz";
        }
        String textContent = "";
        JsonNode quizData = null;
        JsonNode blocks = null;

        if (lesson.getContent() != null && !lesson.getContent().isBlank()) {
            try {
                JsonNode payload = objectMapper.readTree(lesson.getContent());
                if (payload.has("blocks") && payload.get("blocks").isArray()) {
                    blocks = payload.get("blocks");
                    if (blocks.size() > 0) {
                        uiType = blocks.get(0).path("contentType").asText(uiType);
                    }
                }
                if (payload.hasNonNull("uiContentType")) {
                    uiType = payload.get("uiContentType").asText(uiType);
                }
                if (payload.hasNonNull("textContent")) {
                    textContent = payload.get("textContent").asText("");
                } else if (!payload.isObject() && !payload.isArray()) {
                    textContent = lesson.getContent();
                }
                if (payload.has("quizData")) {
                    quizData = payload.get("quizData");
                }
            } catch (JsonProcessingException e) {
                textContent = lesson.getContent();
                if (lesson.getResourceUrl() != null && !lesson.getResourceUrl().isBlank()) {
                    uiType = inferTypeFromUrl(lesson.getResourceUrl());
                } else {
                    uiType = "text";
                }
            }
        } else if (lesson.getResourceUrl() != null && !lesson.getResourceUrl().isBlank()) {
            uiType = inferTypeFromUrl(lesson.getResourceUrl());
        }

        blocks = ensureBlocksArray(
                blocks,
                uiType,
                textContent,
                quizData,
                lesson.getResourceUrl() != null ? lesson.getResourceUrl() : "");

        return new LessonContentPayload(uiType, textContent, quizData, blocks);
    }

    private JsonNode ensureBlocksArray(
            JsonNode blocks, String uiType, String textContent, JsonNode quizData, String resourceUrl) {
        if (blocks != null && blocks.isArray()) {
            return blocks;
        }
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", "legacy-1");
        block.put("contentType", uiType);
        block.put("orderIndex", 0);
        block.put("resourceUrl", resourceUrl != null ? resourceUrl : "");
        block.put("textContent", textContent != null ? textContent : "");
        if (quizData != null && !quizData.isNull()) {
            block.set("quizData", quizData);
        }
        return objectMapper.createArrayNode().add(block);
    }

    private String primaryTypeFromBlocks(JsonNode blocks) {
        if (blocks != null && blocks.isArray() && blocks.size() > 0) {
            return normalizeUiContentType(blocks.get(0).path("contentType").asText("text"));
        }
        return "text";
    }

    private CurriculumLessonResponseDTO toLessonResponse(Lesson lesson) {
        CurriculumLessonResponseDTO dto = new CurriculumLessonResponseDTO();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setOrderIndex(lesson.getOrderIndex());

        LessonContentPayload payload = parseLessonContent(lesson);
        String resolvedResource = mediaStorageService.resolveMediaAccessUrl(
                lesson.getResourceUrl() != null ? lesson.getResourceUrl() : "");
        dto.setResourceUrl(resolvedResource != null ? resolvedResource : "");
        dto.setContentType(payload.uiContentType());
        dto.setTextContent(payload.textContent());
        dto.setQuizData(payload.quizData());
        dto.setBlocks(rewriteBlockMediaUrls(LessonContentNormalizer.normalizeBlocks(payload.blocks())));
        return dto;
    }

    private JsonNode rewriteBlockMediaUrls(JsonNode blocks) {
        if (blocks == null || !blocks.isArray()) {
            return blocks;
        }
        ArrayNode rewritten = objectMapper.createArrayNode();
        for (JsonNode block : blocks) {
            ObjectNode copy = block.deepCopy();
            if (copy.hasNonNull("resourceUrl")) {
                String resolved = mediaStorageService.resolveMediaAccessUrl(copy.get("resourceUrl").asText());
                if (resolved != null) {
                    copy.put("resourceUrl", resolved);
                }
            }
            if (copy.hasNonNull("storageKey")) {
                String resolved = mediaStorageService.resolvePublicUrl(copy.get("storageKey").asText());
                if (resolved != null) {
                    copy.put("resourceUrl", resolved);
                }
            }
            rewritten.add(copy);
        }
        return rewritten;
    }

    private String buildLessonContentFromBlocks(JsonNode blocks, String primaryType) {
        JsonNode normalizedBlocks = LessonContentNormalizer.normalizeBlocks(blocks);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uiContentType", primaryType);
        node.set("blocks", normalizedBlocks);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{\"blocks\":[]}";
        }
    }

    private String buildLessonContent(String uiContentType, String textContent, JsonNode quizData) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", "legacy-1");
        block.put("contentType", uiContentType);
        block.put("orderIndex", 0);
        block.put("resourceUrl", "");
        if (textContent != null && !textContent.isBlank()) {
            block.put("textContent", textContent.trim());
        } else {
            block.put("textContent", "");
        }
        if (quizData != null && !quizData.isNull()) {
            block.set("quizData", quizData);
        }
        return buildLessonContentFromBlocks(objectMapper.createArrayNode().add(block), uiContentType);
    }

    private String normalizeUiContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "video";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String inferTypeFromUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube") || lower.contains("vimeo") || lower.endsWith(".mp4")) {
            return "video";
        }
        if (lower.endsWith(".pdf")) {
            return "pdf";
        }
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return "image";
        }
        if (lower.matches(".*\\.(mp3|wav|ogg)$")) {
            return "audio";
        }
        return "video";
    }

    private Course requireCourseOwned(Long courseId, Long professorId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        if (course.getProfessor() == null || !course.getProfessor().getId().equals(professorId)) {
            throw new AccessDeniedException("You do not own this course");
        }
        return course;
    }
}
