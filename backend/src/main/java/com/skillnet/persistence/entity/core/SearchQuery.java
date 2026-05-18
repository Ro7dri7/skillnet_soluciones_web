package com.skillnet.persistence.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "core_searchquery",
        indexes = {
            @Index(columnList = "query"),
            @Index(columnList = "searched_at"),
            @Index(columnList = "query,searched_at")
        })
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query", length = 200, nullable = false)
    private String query;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "clicked_result", nullable = false)
    private boolean clickedResult;
}
