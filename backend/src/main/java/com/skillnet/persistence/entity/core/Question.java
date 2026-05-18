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
@Table(name = "core_question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "text", columnDefinition = "text", nullable = false)
    private String text;

    @Column(name = "question_type", length = 20, nullable = false)
    private String questionType = "single_choice";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matching_pairs", columnDefinition = "jsonb", nullable = false)
    private JsonNode matchingPairs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ordering_items", columnDefinition = "jsonb", nullable = false)
    private JsonNode orderingItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fill_blank_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode fillBlankData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drag_drop_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode dragDropData;

    @Column(name = "image_url", length = 200)
    private String imageUrl;

    @Column(name = "correct_answer", columnDefinition = "text")
    private String correctAnswer;
}
