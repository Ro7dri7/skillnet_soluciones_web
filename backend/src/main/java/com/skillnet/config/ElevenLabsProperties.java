package com.skillnet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "skillnet.elevenlabs")
public class ElevenLabsProperties {
    private String apiKey = "";
    private String voicePerson1 = "JBFqnCBsd6RMkjVDRZzb";
    private String voicePerson2 = "cgSgspJ2msm6clMCkdW9";
    private String modelId = "eleven_multilingual_v2";
}
