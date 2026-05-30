package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Usuario de aplicación (AUTH_USER_MODEL = core.User). Tabla {@code core_user}.
 * <p>Las tablas M2M de Django {@code auth_groups} / permisos no se modelan aquí salvo que se requieran en fases posteriores.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "password", length = 128, nullable = false)
    private String password;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "is_superuser", nullable = false)
    private boolean superUser;

    @Column(name = "username", length = 150, nullable = false, unique = true)
    private String username;

    @Column(name = "first_name", length = 30)
    private String firstName;

    @Column(name = "last_name", length = 30)
    private String lastName;

    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @Column(name = "is_staff", nullable = false)
    private boolean staff;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "date_joined", nullable = false)
    private Instant dateJoined;

    @Column(name = "role", length = 15, nullable = false)
    private String role = "student";

    @Column(name = "is_student", nullable = false)
    private boolean student = true;

    @Column(name = "is_infoproductor", nullable = false)
    private boolean infoproductor;

    @Column(name = "is_affiliate", nullable = false)
    private boolean affiliate;

    @Column(name = "active_role", length = 15, nullable = false)
    private String activeRole = "student";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    @Column(name = "verification_expires")
    private Instant verificationExpires;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "professional_title", length = 200)
    private String professionalTitle;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specialties", columnDefinition = "jsonb", nullable = false)
    private JsonNode specialties;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb", nullable = false)
    private JsonNode socialLinks;

    @Column(name = "profile_visibility", nullable = false)
    private boolean profileVisibility = true;

    @ManyToMany
    @JoinTable(
            name = "core_user_featured_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> featuredCourses = new HashSet<>();

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "linkedin_url", length = 200)
    private String linkedinUrl;

    @Column(name = "twitter_url", length = 200)
    private String twitterUrl;

    @Column(name = "youtube_url", length = 200)
    private String youtubeUrl;

    @Column(name = "instagram_url", length = 200)
    private String instagramUrl;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified;

    @Column(name = "identity_verified_at")
    private Instant identityVerifiedAt;

    @Column(name = "is_verified_infoproductor", nullable = false)
    private boolean verifiedInfoproductor;

    @Column(name = "verified_until")
    private Instant verifiedUntil;

    @Column(name = "identity_provider", length = 30)
    private String identityProvider = "aws_rekognition";

    @Column(name = "identity_reference", length = 255)
    private String identityReference;

    @Column(name = "behavior_verified", nullable = false)
    private boolean behaviorVerified;

    @Column(name = "behavior_verified_at")
    private Instant behaviorVerifiedAt;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address", length = 255)
    private String address;

    @PrePersist
    @PreUpdate
    private void ensureJsonDefaults() {
        if (specialties == null) {
            specialties = JsonNodeFactory.instance.objectNode();
        }
        if (socialLinks == null) {
            socialLinks = JsonNodeFactory.instance.objectNode();
        }
    }
}
