package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillnet.web.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequestDTO {

    @NotBlank(groups = OnCreate.class, message = "password is required")
    @Size(max = 128, groups = OnCreate.class)
    private String password;

    @NotBlank(message = "username is required")
    @Size(max = 150)
    private String username;

    @Size(max = 30)
    private String firstName;

    @Size(max = 30)
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 254)
    private String email;

    private boolean staff;
    private boolean active;
    private Instant dateJoined;

    @NotBlank(message = "role is required")
    @Size(max = 15)
    private String role;

    private boolean student;
    private boolean infoproductor;
    private boolean affiliate;

    @NotBlank(message = "activeRole is required")
    @Size(max = 15)
    private String activeRole;

    @Size(max = 20)
    private String phone;

    @Size(max = 3)
    private String countryCode;

    private boolean phoneVerified;
    private boolean emailVerified;
    private LocalDate birthDate;

    @Size(max = 500)
    private String logo;

    @Size(max = 500)
    private String profilePicture;

    private String bio;

    @Size(max = 200)
    private String professionalTitle;

    private int yearsExperience;
    private JsonNode specialties;
    private JsonNode socialLinks;
    private boolean profileVisibility;

    @Size(max = 200)
    private String company;

    @Size(max = 200)
    private String location;

    @Size(max = 200)
    private String website;

    @Size(max = 200)
    private String linkedinUrl;

    @Size(max = 200)
    private String twitterUrl;

    @Size(max = 200)
    private String youtubeUrl;

    @Size(max = 200)
    private String instagramUrl;

    @Size(max = 50)
    private String documentNumber;

    private boolean identityVerified;
    private Instant identityVerifiedAt;
    private boolean verifiedInfoproductor;
    private Instant verifiedUntil;

    @Size(max = 30)
    private String identityProvider;

    @Size(max = 255)
    private String identityReference;

    private boolean behaviorVerified;
    private Instant behaviorVerifiedAt;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 255)
    private String address;
}
