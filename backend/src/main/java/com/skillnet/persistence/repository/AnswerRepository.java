package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Answer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySubmission_IdOrderByIdAsc(Long submissionId);

    @Query("""
            SELECT a FROM Answer a
            JOIN FETCH a.question q
            WHERE a.submission.id = :submissionId
            ORDER BY a.id ASC
            """)
    List<Answer> findBySubmissionIdWithQuestion(@Param("submissionId") Long submissionId);

    Optional<Answer> findByIdAndSubmission_Id(Long answerId, Long submissionId);
}
