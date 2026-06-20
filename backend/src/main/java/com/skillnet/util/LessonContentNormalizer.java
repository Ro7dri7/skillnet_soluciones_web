package com.skillnet.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;

/** Evita persistir bloques vacíos del builder (alineado con Lernymart). */
public final class LessonContentNormalizer {

    private static final Set<String> MEDIA_TYPES = Set.of("pdf", "video", "audio", "image", "quiz", "coursebox", "text");

    private LessonContentNormalizer() {}

    public static JsonNode normalizeBlocks(JsonNode blocks) {
        if (blocks == null || !blocks.isArray()) {
            return blocks;
        }
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        for (JsonNode block : blocks) {
            if (blockHasMeaningfulContent(block)) {
                result.add(block);
            }
        }
        return result;
    }

    public static String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed.isBlank() ? "" : content;
        }
        return content;
    }

    public static boolean blockHasMeaningfulContent(JsonNode block) {
        if (block == null || !block.isObject()) {
            return false;
        }

        String blockType = textOrEmpty(block.get("contentType"));
        if (blockType.isBlank()) {
            blockType = textOrEmpty(block.get("type"));
        }

        if ("quiz".equalsIgnoreCase(blockType)) {
            JsonNode quizData = block.get("quizData");
            if (quizData != null && !quizData.isNull() && !quizData.isEmpty()) {
                return true;
            }
            String value = textOrEmpty(block.get("value"));
            return !value.isBlank() && !"{}".equals(value) && !"[]".equals(value);
        }

        if (block.hasNonNull("resourceUrl") && !textOrEmpty(block.get("resourceUrl")).isBlank()) {
            return true;
        }
        if (block.hasNonNull("storageKey") && !textOrEmpty(block.get("storageKey")).isBlank()) {
            return true;
        }

        String textContent = textOrEmpty(block.get("textContent"));
        if (!textContent.isBlank()) {
            return true;
        }

        String value = textOrEmpty(block.get("value"));
        if (!value.isBlank()) {
            return true;
        }

        if (MEDIA_TYPES.contains(blockType.toLowerCase())) {
            return false;
        }

        return !value.isBlank();
    }

    public static ObjectNode normalizeContentObject(ObjectNode node) {
        if (node == null) {
            return node;
        }
        JsonNode blocks = node.get("blocks");
        if (blocks != null && blocks.isArray()) {
            node.set("blocks", normalizeBlocks(blocks));
        }
        return node;
    }

    private static String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }
}
