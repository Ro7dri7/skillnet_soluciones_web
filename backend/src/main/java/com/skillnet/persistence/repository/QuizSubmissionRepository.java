package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.QuizSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    long countByUser_IdAndQuiz_Id(Long userId, Long quizId);

    List<QuizSubmission> findByUser_IdAndQuiz_IdOrderByCreatedAtDesc(Long userId, Long quizId);

    List<QuizSubmission> findByQuiz_Course_Professor_IdOrderByCreatedAtDesc(Long professorId);
}
