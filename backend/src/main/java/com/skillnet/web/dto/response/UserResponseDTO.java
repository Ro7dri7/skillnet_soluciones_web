package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDTO {

    private Long id;
    private Instant lastLogin;
    private boolean superUser;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean staff;
    private boolean active;
    private Instant dateJoined;
    private String role;
    private boolean student;
    private boolean infoproductor;
    private boolean affiliate;
    private String activeRole;
    private String phone;
    private String countryCode;
    private boolean phoneVerified;
    private boolean emailVerified;
    private LocalDate birthDate;
    private String logo;
    private String profilePicture;
    private String bio;
    private String professionalTitle;
    private int yearsExperience;
    private JsonNode specialties;
    private JsonNode socialLinks;
    private boolean profileVisibility;
    private String company;
    private String location;
    private String website;
    private String linkedinUrl;
    private String twitterUrl;
    private String youtubeUrl;
    private String instagramUrl;
    private String documentNumber;
    private boolean identityVerified;
    private Instant identityVerifiedAt;
    private boolean verifiedInfoproductor;
    private Instant verifiedUntil;
    private String identityProvider;
    private String identityReference;
    private boolean behaviorVerified;
    private Instant behaviorVerifiedAt;
    private String postalCode;
    private String address;
}
