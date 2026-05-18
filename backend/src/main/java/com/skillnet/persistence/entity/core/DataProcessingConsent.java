package com.skillnet.persistence.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_dataprocessingconsent")
public class DataProcessingConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    @Column(name = "data_sharing_consent", nullable = false)
    private boolean dataSharingConsent;

    @Column(name = "analytics_consent", nullable = false)
    private boolean analyticsConsent = true;

    @Column(name = "consent_date", nullable = false)
    private Instant consentDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
