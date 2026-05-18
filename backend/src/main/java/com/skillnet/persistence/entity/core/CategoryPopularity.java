package com.skillnet.persistence.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_categorypopularity")
public class CategoryPopularity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", length = 100, nullable = false, unique = true)
    private String category;

    @Column(name = "search_count", nullable = false)
    private int searchCount;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "click_count", nullable = false)
    private int clickCount;

    @Column(name = "last_accessed", nullable = false)
    private Instant lastAccessed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
