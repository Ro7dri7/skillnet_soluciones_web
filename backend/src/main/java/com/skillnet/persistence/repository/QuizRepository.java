package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Quiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourse_Id(Long courseId);
}
