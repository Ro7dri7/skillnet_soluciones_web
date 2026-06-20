package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PublicInfoproductorProfileResponseDTO {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String bio;
    private String professionalTitle;
    private int yearsExperience;
    private JsonNode specialties;
    private JsonNode socialLinks;
    private String company;
    private String location;
    private String website;
    private String linkedinUrl;
    private String twitterUrl;
    private String youtubeUrl;
    private String instagramUrl;
    private String profilePicture;
    private boolean verifiedInfoproductor;
    private List<CourseSummaryDTO> publishedCourses;
}
