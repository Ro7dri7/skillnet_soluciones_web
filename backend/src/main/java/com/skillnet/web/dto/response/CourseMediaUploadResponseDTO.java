package com.skillnet.web.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseMediaUploadResponseDTO {

    private String kind;
    private String storageKey;
    private String publicUrl;
    private String imageUrl;
    private String videoUrl;
}
