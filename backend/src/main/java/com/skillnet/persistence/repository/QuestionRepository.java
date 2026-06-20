package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuiz_IdOrderByIdAsc(Long quizId);

    long countByQuiz_Id(Long quizId);
}
