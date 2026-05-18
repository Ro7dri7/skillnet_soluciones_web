package com.skillnet.service.impl;

import com.skillnet.mapper.LessonMapper;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.service.LessonService;
import com.skillnet.web.dto.request.LessonRequestDTO;
import com.skillnet.web.dto.response.LessonResponseDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;

    public LessonServiceImpl(LessonRepository lessonRepository, LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
    }

    @Override
    @Transactional
    public LessonResponseDTO create(LessonRequestDTO dto) {
        Lesson lesson = lessonMapper.toEntity(dto);
        return lessonMapper.toResponseDTO(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public Optional<LessonResponseDTO> update(Long id, LessonRequestDTO dto) {
        return lessonRepository.findById(id).map(existing -> {
            lessonMapper.applyToEntity(existing, dto, false);
            return lessonMapper.toResponseDTO(lessonRepository.save(existing));
        });
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        lessonRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonResponseDTO> findById(Long id) {
        return lessonRepository.findById(id).map(lessonMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDTO> findAll() {
        return lessonRepository.findAll().stream().map(lessonMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDTO> findByCourseId(Long courseId) {
        return lessonRepository.findByCourse_Id(courseId).stream()
                .map(lessonMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDTO> findByCourseIdOrdered(Long courseId) {
        return lessonRepository.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .map(lessonMapper::toResponseDTO)
                .toList();
    }
}
