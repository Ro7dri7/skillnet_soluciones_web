package com.skillnet.service.producer;

import com.skillnet.config.ElevenLabsProperties;
import com.skillnet.service.ai.OpenRouterClient;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.PodcastGeneration;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.PodcastGenerationRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.service.media.StoredMedia;
import com.skillnet.web.dto.request.PodcastAttachRequestDTO;
import com.skillnet.web.dto.request.PodcastGenerateRequestDTO;
import com.skillnet.web.dto.response.PodcastJobResponseDTO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PodcastGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PodcastGenerationService.class);
    private static final Pattern PERSON_TURN =
            Pattern.compile("<Person([12])>\\s*(.*?)(?=\\s*<Person[12]>|\\Z)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ElevenLabsProperties elevenLabsProperties;
    private final OpenRouterClient openRouterClient;
    private final PodcastGenerationRepository podcastGenerationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final MediaStorageService mediaStorageService;
    private final PodcastJobProcessor podcastJobProcessor;
    private final com.skillnet.service.entitlement.ServiceEntitlementService entitlementService;

    public PodcastGenerationService(
            ElevenLabsProperties elevenLabsProperties,
            OpenRouterClient openRouterClient,
            PodcastGenerationRepository podcastGenerationRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            MediaStorageService mediaStorageService,
            @Lazy PodcastJobProcessor podcastJobProcessor,
            com.skillnet.service.entitlement.ServiceEntitlementService entitlementService) {
        this.elevenLabsProperties = elevenLabsProperties;
        this.openRouterClient = openRouterClient;
        this.podcastGenerationRepository = podcastGenerationRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.mediaStorageService = mediaStorageService;
        this.podcastJobProcessor = podcastJobProcessor;
        this.entitlementService = entitlementService;
    }

    @Transactional
    public PodcastJobResponseDTO startGeneration(Long userId, PodcastGenerateRequestDTO request) {
        if (!request.isTranscriptOnly()
                && (elevenLabsProperties.getApiKey() == null || elevenLabsProperties.getApiKey().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "ELEVENLABS_API_KEY no configurada en el servidor");
        }

        String sourceText = buildSourceText(request);
        if (sourceText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica un tema o texto fuente");
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!request.isTranscriptOnly()) {
            entitlementService.consumeUse(
                    userId, com.skillnet.service.entitlement.ServiceEntitlementService.CAPABILITY_PODCAST_AI);
        }

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository
                    .findById(request.getCourseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
            if (course.getProfessor() == null || !course.getProfessor().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para este curso");
            }
        }

        PodcastGeneration job = new PodcastGeneration();
        job.setRequestedBy(user);
        job.setCourse(course);
        job.setLessonId(request.getLessonId());
        job.setSourceText(sourceText);
        job.setTranscriptOnly(request.isTranscriptOnly());
        job.setLanguage(normalizeLanguage(request.getLanguage()));
        job.setDurationMinutes(normalizeDurationMinutes(request.getDurationMinutes()));
        job.setStatus(PodcastGeneration.STATUS_PENDING);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        PodcastGeneration saved = podcastGenerationRepository.save(job);

        scheduleJobProcessing(saved.getId());

        return toResponse(saved);
    }

    private void scheduleJobProcessing(Long jobId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    podcastJobProcessor.runAsync(jobId);
                }
            });
            return;
        }
        podcastJobProcessor.runAsync(jobId);
    }

    @Transactional
    public void processJob(Long jobId) {
        PodcastGeneration job = podcastGenerationRepository
                .findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job no encontrado"));

        job.setStatus(PodcastGeneration.STATUS_PROCESSING);
        job.setUpdatedAt(Instant.now());
        podcastGenerationRepository.save(job);

        try {
            String transcript = job.getTranscriptText();
            if (transcript == null || transcript.isBlank()) {
                transcript = buildPodcastScript(job.getSourceText(), job.getLanguage(), job.getDurationMinutes());
                job.setTranscriptText(transcript);
            }

            if (job.isTranscriptOnly()) {
                job.setStatus(PodcastGeneration.STATUS_COMPLETED);
                job.setUpdatedAt(Instant.now());
                podcastGenerationRepository.save(job);
                return;
            }

            byte[] audio = synthesizeAudio(transcript);
            Long courseId = job.getCourse() != null ? job.getCourse().getId() : 0L;
            StoredMedia stored = mediaStorageService.storeBytes(
                    courseId, "podcast-ai", "podcast-" + jobId + ".mp3", "audio/mpeg", audio);

            job.setAudioStorageKey(stored.storageKey());
            job.setAudioPublicUrl(stored.publicUrl());
            job.setStatus(PodcastGeneration.STATUS_COMPLETED);
            job.setErrorMessage(null);
        } catch (Exception ex) {
            job.setStatus(PodcastGeneration.STATUS_FAILED);
            job.setErrorMessage(sanitizeError(ex));
            log.warn("Podcast generation failed job={}: {}", jobId, job.getErrorMessage());
        }
        job.setUpdatedAt(Instant.now());
        podcastGenerationRepository.save(job);
    }

    @Transactional
    public void markJobFailed(Long jobId, Exception ex) {
        podcastGenerationRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(PodcastGeneration.STATUS_FAILED);
            job.setErrorMessage(sanitizeError(ex));
            job.setUpdatedAt(Instant.now());
            podcastGenerationRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public PodcastJobResponseDTO getJob(Long userId, Long jobId) {
        PodcastGeneration job = podcastGenerationRepository
                .findByIdAndRequestedBy_Id(jobId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajo no encontrado"));
        return toResponse(job);
    }

    /** Fase 2: genera audio a partir del guion aprobado (consume 1 uso de podcast IA). */
    @Transactional
    public PodcastJobResponseDTO synthesizeFromScript(Long userId, Long jobId, String approvedTranscript) {
        if (elevenLabsProperties.getApiKey() == null || elevenLabsProperties.getApiKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "ELEVENLABS_API_KEY no configurada en el servidor");
        }
        String script = approvedTranscript != null ? approvedTranscript.trim() : "";
        if (script.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El guion aprobado no puede estar vacío");
        }

        PodcastGeneration job = podcastGenerationRepository
                .findByIdAndRequestedBy_Id(jobId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajo no encontrado"));

        if (!job.isTranscriptOnly() || !PodcastGeneration.STATUS_COMPLETED.equals(job.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Este trabajo no está listo para sintetizar audio");
        }

        entitlementService.consumeUse(
                userId, com.skillnet.service.entitlement.ServiceEntitlementService.CAPABILITY_PODCAST_AI);

        job.setSourceText(script);
        job.setTranscriptText(script);
        job.setTranscriptOnly(false);
        job.setStatus(PodcastGeneration.STATUS_PENDING);
        job.setAudioPublicUrl(null);
        job.setAudioStorageKey(null);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());
        podcastGenerationRepository.save(job);

        scheduleJobProcessing(job.getId());
        return toResponse(job);
    }

    @Transactional
    public Map<String, Object> attachToLesson(Long userId, Long jobId, PodcastAttachRequestDTO request) {
        PodcastGeneration job = podcastGenerationRepository
                .findByIdAndRequestedBy_Id(jobId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajo no encontrado"));

        if (!PodcastGeneration.STATUS_COMPLETED.equals(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El podcast aún no está listo");
        }
        if (job.isTranscriptOnly() || job.getAudioPublicUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este trabajo no incluye audio");
        }

        Long lessonId = request.getLessonId() != null ? request.getLessonId() : job.getLessonId();
        if (lessonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lessonId es obligatorio");
        }

        Lesson lesson = lessonRepository
                .findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        if (job.getCourse() != null
                && !lesson.getCourse().getId().equals(job.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lección no pertenece al curso");
        }
        if (!lesson.getCourse().getProfessor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        lesson.setResourceUrl(job.getAudioPublicUrl());
        lesson.setContentType("audio");
        lesson.setUpdatedAt(Instant.now());
        lessonRepository.save(lesson);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("lessonId", lesson.getId());
        result.put("audioUrl", job.getAudioPublicUrl());
        return result;
    }

    private byte[] synthesizeAudio(String transcript) {
        List<Map.Entry<String, String>> turns = parsePersonDialogue(transcript);
        if (turns.isEmpty()) {
            turns = List.of(Map.entry("person1", stripPersonTags(transcript)));
        }

        Map<String, String> voices = Map.of(
                "person1", elevenLabsProperties.getVoicePerson1(),
                "person2", elevenLabsProperties.getVoicePerson2());

        List<byte[]> segments = new ArrayList<>();
        for (Map.Entry<String, String> turn : turns) {
            String voiceId = voices.getOrDefault(turn.getKey(), voices.get("person1"));
            segments.add(textToSpeech(turn.getValue(), voiceId));
        }

        int total = segments.stream().mapToInt(segment -> segment.length).sum();
        byte[] combined = new byte[total];
        int offset = 0;
        for (byte[] segment : segments) {
            System.arraycopy(segment, 0, combined, offset, segment.length);
            offset += segment.length;
        }
        return combined;
    }

    private byte[] textToSpeech(String text, String voiceId) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isBlank()) {
            return new byte[0];
        }

        Map<String, Object> body = Map.of(
                "text", trimmed,
                "model_id", elevenLabsProperties.getModelId());

        try {
            return RestClient.create()
                    .post()
                    .uri("https://api.elevenlabs.io/v1/text-to-speech/{voiceId}", voiceId)
                    .header("xi-api-key", elevenLabsProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "ElevenLabs error " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString());
        }
    }

    private List<Map.Entry<String, String>> parsePersonDialogue(String transcript) {
        List<Map.Entry<String, String>> turns = new ArrayList<>();
        Matcher matcher = PERSON_TURN.matcher(transcript);
        while (matcher.find()) {
            String person = "person" + matcher.group(1);
            String body = stripPersonTags(matcher.group(2)).trim();
            if (!body.isBlank()) {
                turns.add(Map.entry(person, body));
            }
        }
        return turns;
    }

    private String stripPersonTags(String text) {
        return text.replaceAll("(?i)</?Person[12]>", "").trim();
    }

    private String buildPodcastScript(String sourceText, String language, Integer durationMinutes) {
        if (sourceText.contains("<Person1>") || sourceText.contains("<Person2>")) {
            return sourceText;
        }

        if (openRouterClient.isConfigured()) {
            try {
                return generateScriptWithOpenRouter(sourceText, language, durationMinutes);
            } catch (ResponseStatusException ex) {
                log.warn("OpenRouter script generation failed: {}", ex.getReason());
                throw ex;
            } catch (Exception ex) {
                log.warn("OpenRouter script generation failed, using template: {}", ex.getMessage());
            }
        }

        return buildTemplatePodcastScript(sourceText, language, durationMinutes);
    }

    private String generateScriptWithOpenRouter(String sourceText, String language, Integer durationMinutes) {
        Map<String, Object> config = loadConversationConfig();
        String podcastName = String.valueOf(config.getOrDefault("podcast_name", "SkillNet"));
        String instructions = String.valueOf(config.getOrDefault("user_instructions", "")).trim();
        int minutes = normalizeDurationMinutes(durationMinutes);
        int targetWords = minutes * 110;
        String languageLabel = languageInstruction(normalizeLanguage(language));

        String system =
                """
                Eres guionista de podcasts formativos corporativos. \
                Tu máxima prioridad es fidelidad al tema indicado en el material fuente. \
                Nunca sustituyas el tema por otro distinto ni inventes episodios genéricos.""";

        String prompt =
                """
                Programa: %s
                Idioma obligatorio del diálogo: %s
                Duración MÁXIMA del episodio: %d minutos (~%d palabras en total entre ambos hablantes).
                %s

                REGLAS ESTRICTAS DE CONTENIDO:
                - Trata SOLO el tema del material fuente (título y contenido).
                - Todo el diálogo debe estar en %s.
                - En la introducción menciona el nombre del programa indicado arriba.
                - Usa exactamente 4 o 5 intercambios cortos y fluidos.
                - Responde ÚNICAMENTE con etiquetas <Person1> y <Person2>.

                MATERIAL FUENTE (obligatorio respetar):
                %s

                Escribe el guion final listo para locución."""
                        .formatted(
                                podcastName,
                                languageLabel,
                                minutes,
                                targetWords,
                                instructions,
                                languageLabel,
                                sourceText);

        return openRouterClient.chatCompletion(prompt, system);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConversationConfig() {
        try (InputStream input = new ClassPathResource("podcast-conversation-config.yaml").getInputStream()) {
            String yaml = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            Object loaded = new Yaml().load(yaml);
            if (loaded instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (Exception ex) {
            log.debug("Could not load podcast conversation config: {}", ex.getMessage());
        }
        return Map.of("podcast_name", "SkillNet");
    }

    private String buildTemplatePodcastScript(String sourceText, String language, Integer durationMinutes) {
        String topic = sourceText.lines().findFirst().orElse(sourceText).trim();
        if (topic.length() > 120) {
            topic = topic.substring(0, 120) + "…";
        }

        String body = sourceText.length() > 800 ? sourceText.substring(0, 800) + "…" : sourceText;
        int minutes = normalizeDurationMinutes(durationMinutes);
        String languageLabel = languageInstruction(normalizeLanguage(language));

        return """
                <Person1> Bienvenidos a SkillNet Podcast. Hoy hablamos de un tema clave (%s, ~%d min).
                <Person2> Exactamente. El foco de este episodio es: %s
                <Person1> %s
                <Person2> Gracias por escucharnos. Sigue aprendiendo con SkillNet.
                """
                .formatted(languageLabel, minutes, topic, body.replace("\n", " "));
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "es";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private int normalizeDurationMinutes(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 1) {
            return 2;
        }
        return Math.min(durationMinutes, 10);
    }

    private String languageInstruction(String languageCode) {
        return switch (languageCode) {
            case "en" -> "English";
            case "pt" -> "Portuguese (Brazilian)";
            default -> "Spanish (Latin American)";
        };
    }

    private String buildSourceText(PodcastGenerateRequestDTO request) {
        String topic = request.getTopic() != null ? request.getTopic().trim() : "";
        String text = request.getText() != null ? request.getText().trim() : "";
        if (!text.isBlank()) {
            if (!topic.isBlank()) {
                return "TÍTULO: " + topic + "\n\nCONTENIDO:\n" + text;
            }
            return text;
        }
        return topic;
    }

    private PodcastJobResponseDTO toResponse(PodcastGeneration job) {
        boolean done = PodcastGeneration.STATUS_COMPLETED.equals(job.getStatus());
        boolean failed = PodcastGeneration.STATUS_FAILED.equals(job.getStatus());
        return PodcastJobResponseDTO.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .transcript(done || failed ? nullToEmpty(job.getTranscriptText()) : "")
                .audioUrl(done ? job.getAudioPublicUrl() : null)
                .transcriptOnly(job.isTranscriptOnly())
                .language(job.getLanguage())
                .durationMinutes(job.getDurationMinutes())
                .errorMessage(failed ? nullToEmpty(job.getErrorMessage()) : "")
                .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeError(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return message.replaceAll("sk_[a-zA-Z0-9_-]{10,}", "[redacted]")
                .replaceAll("xi-api-key[^\\s]*", "[redacted]")
                .substring(0, Math.min(message.length(), 500));
    }
}
