package com.skillnet.web.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MediaUploadResponseDTO {

    private String url;
    private String storageKey;
    private String type;
}
