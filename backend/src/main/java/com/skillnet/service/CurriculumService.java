package com.skillnet.service;

import com.skillnet.web.dto.request.CreateCurriculumLessonRequestDTO;
import com.skillnet.web.dto.request.CreateSectionRequestDTO;
import com.skillnet.web.dto.request.LessonUpdateRequestDTO;
import com.skillnet.web.dto.request.SectionUpdateRequestDTO;
import com.skillnet.web.dto.response.CurriculumLessonResponseDTO;
import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import java.util.List;

public interface CurriculumService {

    List<CurriculumModuleResponseDTO> getCurriculum(Long courseId, Long professorId);

    List<CurriculumModuleResponseDTO> getCurriculumForLearner(Long courseId, Long userId);

    CurriculumModuleResponseDTO createSection(
            Long courseId, Long professorId, CreateSectionRequestDTO request);

    CurriculumLessonResponseDTO createLesson(
            Long sectionId, Long professorId, CreateCurriculumLessonRequestDTO request);

    CurriculumLessonResponseDTO updateLesson(
            Long lessonId, Long professorId, CreateCurriculumLessonRequestDTO request);

    CurriculumLessonResponseDTO patchLesson(
            Long lessonId, Long professorId, LessonUpdateRequestDTO request);

    CurriculumModuleResponseDTO updateSection(
            Long sectionId, Long professorId, SectionUpdateRequestDTO request);

    void deleteSection(Long sectionId, Long professorId);

    void deleteLesson(Long lessonId, Long professorId);
}
