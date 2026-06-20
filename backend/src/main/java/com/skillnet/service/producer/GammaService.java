package com.skillnet.service.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.config.GammaProperties;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.GammaGeneration;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.GammaGenerationRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.service.media.StoredMedia;
import com.skillnet.web.dto.request.GammaGenerateRequestDTO;
import com.skillnet.web.dto.response.GammaGenerationResponseDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Integración Gamma alineada con Lernymart ({@code views_gamma.py} + {@code gamma_service.py}):
 * proxy a la API pública, polling de estado y {@code exportUrl} PDF para vista previa.
 * En local sin {@code GAMMA_API_KEY}, dev-mock guarda un PDF mínimo en SkillNet cuando hay {@code courseId}.
 */
@Service
@RequiredArgsConstructor
public class GammaService {

    private static final Logger log = LoggerFactory.getLogger(GammaService.class);
    private static final String GAMMA_API_URL = "https://public-api.gamma.app/v1.0";

    /** Plantilla PDF mínima; usar {@code {{TITLE}}} — no {@code String.formatted} (rompe con %PDF). */
    private static final String DEV_PDF_TEMPLATE =
            """
            %PDF-1.4
            1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
            2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
            3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj
            4 0 obj<< /Length 44 >>stream
            BT /F1 14 Tf 72 720 Td ({{TITLE}}) Tj ET
            endstream endobj
            5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
            xref
            0 6
            0000000000 65535 f
            0000000010 00000 n
            0000000060 00000 n
            0000000114 00000 n
            0000000270 00000 n
            0000000368 00000 n
            trailer<< /Size 6 /Root 1 0 R >>
            startxref
            445
            %%EOF
            """;

    private final GammaProperties gammaProperties;
    private final GammaGenerationRepository gammaGenerationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final MediaStorageService mediaStorageService;
    private final ObjectMapper objectMapper;
    private final com.skillnet.service.entitlement.ServiceEntitlementService entitlementService;

    @Transactional
    public GammaGenerationResponseDTO startGeneration(Long userId, GammaGenerateRequestDTO request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Long courseId = resolveOptionalCourseId(userId, request.getCourseId());

        entitlementService.consumeUse(userId, com.skillnet.service.entitlement.ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK);

        if (gammaProperties.useDevMock()) {
            log.warn("GAMMA_API_KEY no configurada: ebook simulado (dev-mock)");
            return startDevMockGeneration(user, courseId, request);
        }

        String apiKey = gammaProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GAMMA_API_KEY no configurada. Define GAMMA_API_KEY en el servidor.");
        }

        int pages = request.getPages() != null && request.getPages() > 0 ? request.getPages() : 10;
        String format = request.getFormat() != null && !request.getFormat().isBlank()
                ? request.getFormat()
                : "document";

        Map<String, Object> payload = buildGammaPayload(request, pages, format);

        JsonNode response;
        try {
            response = RestClient.create()
                    .post()
                    .uri(GAMMA_API_URL + "/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-KEY", apiKey)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            log.warn("Gamma POST failed: {}", ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, extractGammaError(ex));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo contactar a Gamma: " + ex.getMessage());
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de Gamma");
        }

        String generationId = extractGenerationId(response);
        if (generationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gamma no devolvió generation id");
        }

        GammaGeneration record = new GammaGeneration();
        record.setUser(user);
        record.setCourseId(courseId);
        record.setLessonId(request.getLessonId());
        record.setGenerationId(generationId);
        record.setStatus(textOrNull(response, "status"));
        record.setGammaUrl(textOrNull(response, "gammaUrl", "gamma_url"));
        record.setExportUrl(textOrNull(response, "exportUrl", "export_url"));
        record.setPrompt(request.getPrompt().trim());
        record.setCreatedAt(Instant.now());
        gammaGenerationRepository.save(record);

        return toResponse(record, response);
    }

    @Transactional
    public GammaGenerationResponseDTO getStatus(Long userId, String generationId) {
        GammaGeneration record = gammaGenerationRepository
                .findByGenerationIdAndUser_Id(generationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Generación no encontrada"));

        if (generationId.startsWith("dev_gamma_")) {
            repairDevMockRecord(record);
            gammaGenerationRepository.save(record);
            return toResponse(record, objectMapper.createObjectNode().put("mode", "dev_mock"));
        }

        if (isPlatformStored(record.getExportUrl())) {
            return toResponse(record, null);
        }

        String apiKey = gammaProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GAMMA_API_KEY no configurada");
        }

        JsonNode response;
        try {
            response = RestClient.create()
                    .get()
                    .uri(GAMMA_API_URL + "/generations/{id}", generationId)
                    .header("X-API-KEY", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, extractGammaError(ex));
        }

        record.setStatus(textOrNull(response, "status"));
        record.setGammaUrl(textOrNull(response, "gammaUrl", "gamma_url"));
        String remoteExport = textOrNull(response, "exportUrl", "export_url");
        if (remoteExport != null && !remoteExport.isBlank()) {
            record.setExportUrl(remoteExport);
        }

        if (record.getCourseId() != null && isCompletedStatus(record.getStatus())) {
            maybeImportPdfToPlatform(record);
        }
        gammaGenerationRepository.save(record);

        return toResponse(record, response);
    }

    private void maybeImportPdfToPlatform(GammaGeneration record) {
        if (isPlatformStored(record.getExportUrl())) {
            return;
        }
        String remoteUrl = record.getExportUrl();
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return;
        }
        if (!isCompletedStatus(record.getStatus())) {
            return;
        }
        try {
            byte[] pdf = RestClient.create().get().uri(remoteUrl).retrieve().body(byte[].class);
            if (pdf == null || pdf.length == 0) {
                return;
            }
            StoredMedia stored = mediaStorageService.storeBytes(
                    record.getCourseId(), "ebooks", "gamma-ebook.pdf", "application/pdf", pdf);
            record.setExportUrl(stored.publicUrl());
            record.setStatus("completed");
        } catch (IOException ex) {
            log.warn("No se pudo importar PDF de Gamma a SkillNet: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Fallo descargando PDF de Gamma: {}", ex.getMessage());
        }
    }

    private void repairDevMockRecord(GammaGeneration record) {
        if (isPlatformStored(record.getExportUrl())) {
            return;
        }
        if (record.getGammaUrl() != null && record.getGammaUrl().contains("gamma.app")) {
            record.setGammaUrl(null);
        }
        Long courseId = record.getCourseId();
        if (courseId == null) {
            log.warn(
                    "Registro dev_gamma {} sin courseId; no se puede generar PDF en SkillNet",
                    record.getGenerationId());
            return;
        }
        String title = record.getPrompt() != null && !record.getPrompt().isBlank()
                ? record.getPrompt().trim()
                : "Ebook SkillNet";
        try {
            byte[] pdf = buildDevPdf(title);
            StoredMedia stored = mediaStorageService.storeBytes(
                    courseId, "ebooks", "gamma-ebook-dev.pdf", "application/pdf", pdf);
            record.setExportUrl(stored.publicUrl());
            record.setStatus("completed");
            log.info("PDF dev-mock reparado para generación {}", record.getGenerationId());
        } catch (IOException ex) {
            log.warn("No se pudo reparar PDF dev-mock: {}", ex.getMessage());
        }
    }

    private GammaGenerationResponseDTO startDevMockGeneration(
            User user, Long courseId, GammaGenerateRequestDTO request) {
        if (courseId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "courseId es obligatorio en modo desarrollo para guardar el PDF en SkillNet.");
        }

        String generationId = "dev_gamma_" + UUID.randomUUID();
        String platformUrl;
        try {
            byte[] pdf = buildDevPdf(
                    request.getTitle() != null && !request.getTitle().isBlank()
                            ? request.getTitle().trim()
                            : request.getPrompt().trim());
            StoredMedia stored =
                    mediaStorageService.storeBytes(courseId, "ebooks", "gamma-ebook-dev.pdf", "application/pdf", pdf);
            platformUrl = stored.publicUrl();
        } catch (IOException ex) {
            log.error("No se pudo crear PDF dev-mock", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el PDF de prueba");
        }

        GammaGeneration record = new GammaGeneration();
        record.setUser(user);
        record.setCourseId(courseId);
        record.setLessonId(request.getLessonId());
        record.setGenerationId(generationId);
        record.setStatus("completed");
        record.setGammaUrl(null);
        record.setExportUrl(platformUrl);
        record.setPrompt(request.getPrompt().trim());
        record.setCreatedAt(Instant.now());
        gammaGenerationRepository.save(record);
        return toResponse(record, objectMapper.createObjectNode().put("mode", "dev_mock"));
    }

    private Map<String, Object> buildGammaPayload(GammaGenerateRequestDTO request, int pages, String format) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("inputText", buildInputText(request));
        payload.put("textMode", "generate");
        payload.put("format", format);
        payload.put("numCards", pages);
        payload.put("exportAs", "pdf");

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            payload.put("title", request.getTitle().trim());
        }

        Map<String, Object> textOptions = new HashMap<>();
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            textOptions.put("language", request.getLanguage().trim());
        }
        if (request.getTone() != null && !request.getTone().isBlank()) {
            textOptions.put("tone", request.getTone().trim());
        }
        if (request.getAudience() != null && !request.getAudience().isBlank()) {
            textOptions.put("audience", request.getAudience().trim());
        }
        if (request.getTextAmount() != null && !request.getTextAmount().isBlank()) {
            textOptions.put("amount", request.getTextAmount().trim());
        }
        if (!textOptions.isEmpty()) {
            payload.put("textOptions", textOptions);
        }

        String imageSource = request.getImageSource() != null && !request.getImageSource().isBlank()
                ? request.getImageSource().trim()
                : "aiGenerated";
        Map<String, Object> imageOptions = new HashMap<>();
        imageOptions.put("source", imageSource);
        if ("aiGenerated".equals(imageSource)
                && request.getImageStyle() != null
                && !request.getImageStyle().isBlank()) {
            imageOptions.put("style", request.getImageStyle().trim());
        }
        payload.put("imageOptions", imageOptions);

        if (request.getAdditionalInstructions() != null && !request.getAdditionalInstructions().isBlank()) {
            payload.put("additionalInstructions", request.getAdditionalInstructions().trim());
        }

        return payload;
    }

    private String buildInputText(GammaGenerateRequestDTO request) {
        StringBuilder sb = new StringBuilder(request.getPrompt().trim());
        if (request.getSourceMaterial() != null && !request.getSourceMaterial().isBlank()) {
            sb.append("\n\n---\nMaterial de referencia (PDF):\n")
                    .append(request.getSourceMaterial().trim());
        }
        return sb.toString();
    }

    private Long resolveOptionalCourseId(Long userId, Long courseId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
        if (course.getProfessor() == null || !course.getProfessor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para este curso");
        }
        return courseId;
    }

    private boolean isPlatformStored(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.contains(MediaStorageService.MEDIA_FILES_PREFIX) || url.startsWith("courses/");
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toLowerCase(Locale.ROOT);
        return normalized.equals("completed")
                || normalized.equals("complete")
                || normalized.equals("succeeded");
    }

    private byte[] buildDevPdf(String title) {
        String safeTitle = escapePdfTitle(title);
        return DEV_PDF_TEMPLATE.replace("{{TITLE}}", safeTitle).getBytes(StandardCharsets.UTF_8);
    }

    private static String escapePdfTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Ebook SkillNet";
        }
        String trimmed = title.length() > 80 ? title.substring(0, 80) : title;
        return trimmed.replace('\\', '/').replace('(', '[').replace(')', ']');
    }

    private GammaGenerationResponseDTO toResponse(GammaGeneration record, JsonNode raw) {
        String exportUrl = record.getExportUrl();
        String platformUrl = isPlatformStored(exportUrl) ? exportUrl : null;
        return GammaGenerationResponseDTO.builder()
                .id(record.getGenerationId())
                .status(record.getStatus())
                .gammaUrl(record.getGammaUrl())
                .exportUrl(exportUrl)
                .platformExportUrl(platformUrl)
                .raw(raw)
                .build();
    }

    private String extractGenerationId(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.hasNonNull("id")) {
            return node.get("id").asText();
        }
        if (node.hasNonNull("generationId")) {
            return node.get("generationId").asText();
        }
        if (node.hasNonNull("generation_id")) {
            return node.get("generation_id").asText();
        }
        return null;
    }

    private String textOrNull(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            if (node.hasNonNull(field)) {
                return node.get(field).asText();
            }
        }
        return null;
    }

    private String extractGammaError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "Gamma HTTP " + ex.getStatusCode().value();
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("message")) {
                return json.get("message").asText();
            }
            if (json.has("error")) {
                return json.get("error").asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }
}
