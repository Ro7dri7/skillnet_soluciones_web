package com.skillnet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "skillnet.openrouter")
public class OpenRouterProperties {
    private String apiKey = "";
    private String model = "google/gemini-2.5-flash-lite";
    private String modelFallback = "openrouter/auto";
    private String referer = "http://localhost:4200";
    private String title = "SkillNet";
}
