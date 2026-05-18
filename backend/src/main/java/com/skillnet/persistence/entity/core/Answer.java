package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private QuizSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Column(name = "text_answer", columnDefinition = "text")
    private String textAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matching_pairs", columnDefinition = "jsonb", nullable = false)
    private JsonNode matchingPairs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ordering_answer", columnDefinition = "jsonb", nullable = false)
    private JsonNode orderingAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fill_blank_answer", columnDefinition = "jsonb", nullable = false)
    private JsonNode fillBlankAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drag_drop_answer", columnDefinition = "jsonb", nullable = false)
    private JsonNode dragDropAnswer;

    @Column(name = "boolean_answer")
    private Boolean booleanAnswer;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "tutor_feedback", columnDefinition = "text")
    private String tutorFeedback;
}
