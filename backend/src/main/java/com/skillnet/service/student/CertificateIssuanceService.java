package com.skillnet.service.student;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.CourseCertificate;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseCertificateRepository;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.ProgressRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.notification.NotificationPublisher;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateIssuanceService {

    private final CourseCertificateRepository courseCertificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final NotificationPublisher notificationPublisher;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public void tryIssueOnCourseCompletion(Long studentId, Long courseId) {
        if (courseCertificateRepository
                .findFirstByCourse_IdAndStudent_IdAndActiveTrue(courseId, studentId)
                .isPresent()) {
            return;
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        User student = userRepository.findById(studentId).orElse(null);
        if (course == null || student == null) {
            return;
        }

        long totalLessons = lessonRepository.countByCourse_Id(courseId);
        if (totalLessons == 0) {
            return;
        }

        long completed = progressRepository.countCompletedByUserAndCourse(studentId, courseId);
        if (completed < totalLessons) {
            return;
        }

        User issuer = course.getProfessor() != null ? course.getProfessor() : student;
        String verificationCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        CourseCertificate certificate = new CourseCertificate();
        certificate.setCourse(course);
        certificate.setStudent(student);
        certificate.setUploadedBy(issuer);
        certificate.setCertificateFile("auto:" + verificationCode);
        certificate.setUploadedAt(Instant.now());
        certificate.setActive(true);
        certificate.setSecurityStatus("verified");
        courseCertificateRepository.save(certificate);

        String learnLink = frontendBaseUrl + "/certificates";

        notificationPublisher.publish(
                student,
                "certificate",
                "Certificado disponible",
                "Completaste \"" + course.getTitle() + "\". Tu certificado ya está emitido.",
                learnLink);
    }
}
