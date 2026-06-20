package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserProfileRequestDTO {

    @Size(max = 30)
    private String firstName;

    @Size(max = 30)
    private String lastName;

    private String bio;

    @Size(max = 200)
    private String professionalTitle;

    private Integer yearsExperience;
    private JsonNode specialties;
    private JsonNode socialLinks;
    private Boolean profileVisibility;

    @Size(max = 20)
    private String phone;

    @Size(max = 3)
    private String countryCode;

    private LocalDate birthDate;

    @Size(max = 500)
    private String logo;

    @Size(max = 500)
    private String profilePicture;

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

    @Size(max = 20)
    private String postalCode;

    @Size(max = 255)
    private String address;
}
