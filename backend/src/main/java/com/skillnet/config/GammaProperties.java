package com.skillnet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "skillnet.gamma")
public class GammaProperties {
    private String apiKey = "";
    /** Si true y no hay api-key, simula generación (solo desarrollo). */
    private boolean devMock = true;

    public boolean useDevMock() {
        return devMock && (apiKey == null || apiKey.isBlank());
    }
}
