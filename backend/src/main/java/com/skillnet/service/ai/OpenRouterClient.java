package com.skillnet.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.config.OpenRouterProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpenRouterClient {

    private static final String CHAT_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenRouterClient(OpenRouterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public String chatCompletion(String prompt, String system) {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENROUTER_API_KEY no configurada en el servidor");
        }

        List<String> models = new ArrayList<>();
        if (properties.getModel() != null && !properties.getModel().isBlank()) {
            models.add(properties.getModel().trim());
        }
        if (properties.getModelFallback() != null
                && !properties.getModelFallback().isBlank()
                && !models.contains(properties.getModelFallback().trim())) {
            models.add(properties.getModelFallback().trim());
        }
        if (models.isEmpty()) {
            models.add("google/gemini-2.5-flash-lite");
        }

        String lastError = "";
        for (String model : models) {
            try {
                return requestModel(prompt, system, model);
            } catch (RestClientResponseException ex) {
                lastError = ex.getResponseBodyAsString();
                if (ex.getStatusCode().value() == 429 || ex.getStatusCode().value() == 404) {
                    continue;
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenRouter error " + ex.getStatusCode().value() + ": " + sanitize(lastError));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "OpenRouter agotó modelos disponibles: " + sanitize(lastError));
    }

    private String requestModel(String prompt, String system, String model) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);

        String responseBody = restClient
                .post()
                .uri(CHAT_URL)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("HTTP-Referer", properties.getReferer())
                .header("X-Title", properties.getTitle())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.isTextual() ? content.asText().trim() : "";
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenRouter devolvió un guion vacío");
            }
            return text;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta OpenRouter inválida");
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("sk_[a-zA-Z0-9_-]{10,}", "[redacted]")
                .replaceAll("sk-or-v1-[a-zA-Z0-9]{10,}", "[redacted]")
                .substring(0, Math.min(value.length(), 300));
    }
}
