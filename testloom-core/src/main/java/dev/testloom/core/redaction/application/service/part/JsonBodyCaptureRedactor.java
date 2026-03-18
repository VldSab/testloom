package dev.testloom.core.redaction.application.service.part;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Objects;

/**
 * Redacts JSON body fields using compiled policy.
 *
 * <p>Masking contract: masked JSON fields are replaced with string values.
 * This is intentional to make sensitive data replacement explicit and deterministic
 * regardless of original JSON type (number/boolean/object/array).
 *
 * <p>Fallback contract: when body cannot be treated as JSON (content-type/body
 * shape mismatch, parse/write error), per-target default action for JSON_FIELD
 * is applied to the whole body string (KEEP/MASK/REMOVE).
 */
@Slf4j
public final class JsonBodyCaptureRedactor {
    private final ObjectMapper objectMapper;
    private final RedactionPolicy policy;

    public JsonBodyCaptureRedactor(ObjectMapper objectMapper, RedactionPolicy policy) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public String redactIfJson(String body, String contentType) {
        if (body == null || body.isBlank()) {
            return body;
        }
        if (!looksLikeJson(contentType, body)) {
            return applyBodyFallbackDecision(body);
        }
        try {
            JsonNode source = objectMapper.readTree(body);
            if (source == null) {
                return applyBodyFallbackDecision(body);
            }
            JsonNode redacted = redactNode(source);
            return objectMapper.writeValueAsString(redacted);
        } catch (Exception exception) {
            if (exception instanceof RedactionException redactionException) {
                throw redactionException;
            }
            log.debug("JSON body redaction skipped due to parse/write failure.", exception);
            return applyBodyFallbackDecision(body);
        }
    }

    private JsonNode redactNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            return redactObject((ObjectNode) node);
        }
        if (node.isArray()) {
            return redactArray((ArrayNode) node);
        }
        return node;
    }

    private ObjectNode redactObject(ObjectNode source) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        source.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            RedactionDecision decision = policy.resolve(RedactionTargetType.JSON_FIELD, fieldName);
            switch (decision) {
                case null -> throw new RedactionException("Unsupported redaction decision: null");
                case RedactionDecision.Remove ignored -> {
                    // no-op, field dropped
                }
                case RedactionDecision.Mask(String replacement) ->
                        objectNode.set(fieldName, TextNode.valueOf(replacement));
                case RedactionDecision.Keep ignored ->
                        objectNode.set(fieldName, redactNode(entry.getValue()));
            }
        });
        return objectNode;
    }

    private ArrayNode redactArray(ArrayNode source) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JsonNode child : source) {
            arrayNode.add(redactNode(child));
        }
        return arrayNode;
    }

    private String applyBodyFallbackDecision(String body) {
        RedactionDecision decision = policy.resolve(RedactionTargetType.JSON_FIELD, null);
        return switch (decision) {
            case null -> throw new RedactionException("Unsupported redaction decision: null");
            case RedactionDecision.Keep ignored -> body;
            case RedactionDecision.Remove ignored -> null;
            case RedactionDecision.Mask(String replacement) -> replacement;
        };
    }

    private boolean looksLikeJson(String contentType, String body) {
        if (contentType != null && !contentType.isBlank() && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            return true;
        }
        String trimmed = body.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
