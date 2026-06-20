package com.skillnet.service.student;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.CourseCertificate;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.repository.CourseCertificateRepository;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.ProgressRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.web.dto.response.CertificateCheckResponseDTO;
import com.skillnet.web.dto.response.CertificateItemDTO;
import com.skillnet.web.dto.response.CertificateOverviewResponseDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CourseCertificateRepository courseCertificateRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final CertificateIssuanceService certificateIssuanceService;

    @Transactional(readOnly = true)
    public CertificateOverviewResponseDTO getOverview(Long studentId) {
        List<CertificateItemDTO> certificates =
                courseCertificateRepository.findByStudent_IdAndActiveTrue(studentId).stream()
                        .map(this::toItem)
                        .toList();

        return CertificateOverviewResponseDTO.builder()
                .totalCertificates(certificates.size())
                .certificates(certificates)
                .build();
    }

    @Transactional
    public CertificateCheckResponseDTO checkCourseCertificate(Long studentId, Long courseId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        var certificateOpt = courseCertificateRepository.findFirstByCourse_IdAndStudent_IdAndActiveTrue(
                courseId, studentId);

        if (certificateOpt.isPresent()) {
            return CertificateCheckResponseDTO.builder()
                    .courseId(courseId)
                    .hasCertificate(true)
                    .eligible(true)
                    .message("Certificado disponible")
                    .certificate(toItem(certificateOpt.get()))
                    .build();
        }

        boolean eligible = isEligibleForCertificate(studentId, course);
        if (eligible) {
            certificateIssuanceService.tryIssueOnCourseCompletion(studentId, courseId);
            certificateOpt = courseCertificateRepository.findFirstByCourse_IdAndStudent_IdAndActiveTrue(
                    courseId, studentId);
            if (certificateOpt.isPresent()) {
                return CertificateCheckResponseDTO.builder()
                        .courseId(courseId)
                        .hasCertificate(true)
                        .eligible(true)
                        .message("Certificado emitido al completar el curso")
                        .certificate(toItem(certificateOpt.get()))
                        .build();
            }
        }

        String message = eligible
                ? "Completaste el curso; el certificado se está procesando"
                : "Aún no cumples los requisitos para el certificado";

        return CertificateCheckResponseDTO.builder()
                .courseId(courseId)
                .hasCertificate(false)
                .eligible(eligible)
                .message(message)
                .certificate(null)
                .build();
    }

    private boolean isEligibleForCertificate(Long studentId, Course course) {
        Enrollment enrollment = enrollmentRepository
                .findByUser_IdAndCourse_Id(studentId, course.getId())
                .orElse(null);
        if (enrollment == null) {
            return false;
        }
        if (enrollment.isCompleted()) {
            return true;
        }
        long totalLessons = lessonRepository.countByCourse_Id(course.getId());
        if (totalLessons == 0) {
            return false;
        }
        long completed = progressRepository.countCompletedByUserAndCourse(studentId, course.getId());
        return completed >= totalLessons;
    }

    private CertificateItemDTO toItem(CourseCertificate certificate) {
        Course course = certificate.getCourse();
        return CertificateItemDTO.builder()
                .id(certificate.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .certificateFile(certificate.getCertificateFile())
                .uploadedAt(certificate.getUploadedAt())
                .build();
    }
}
