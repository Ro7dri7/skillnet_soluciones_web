package com.skillnet.service.producer;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.security.JwtService;
import com.skillnet.web.dto.response.ReferralLinkResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final CourseRepository courseRepository;
    private final JwtService jwtService;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public ReferralLinkResponseDTO getReferralLink(Long professorId, Long courseId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (course.getProfessor() == null || !course.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        String token = jwtService.generateReferralToken(courseId, professorId);
        String slug = course.getSlug() != null ? course.getSlug() : String.valueOf(courseId);
        String url = frontendBaseUrl + "/courses/" + slug + "?ref=" + token;

        return ReferralLinkResponseDTO.builder()
                .courseId(courseId)
                .token(token)
                .url(url)
                .build();
    }
}
