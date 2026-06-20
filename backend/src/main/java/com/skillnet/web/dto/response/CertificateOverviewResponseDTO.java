package com.skillnet.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CertificateOverviewResponseDTO {

    private int totalCertificates;
    private List<CertificateItemDTO> certificates;
}
