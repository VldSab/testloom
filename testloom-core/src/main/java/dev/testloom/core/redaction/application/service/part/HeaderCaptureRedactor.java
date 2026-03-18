package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Redacts header map using compiled policy.
 *
 * <p>All incoming headers are evaluated through {@link RedactionPolicy}:
 * KEEP keeps value, MASK replaces value, REMOVE drops header.
 * If a header has empty value list, mask action preserves empty list shape.
 */
public final class HeaderCaptureRedactor {
    private final RedactionPolicy policy;

    public HeaderCaptureRedactor(RedactionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public Map<String, List<String>> redact(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> redacted = new LinkedHashMap<>();
        for (var entry : headers.entrySet()) {
            String headerName = entry.getKey();
            List<String> normalizedValues = normalizeValues(entry.getValue());
            RedactionDecision decision = policy.resolve(RedactionTargetType.HEADER, headerName);
            switch (decision) {
                case null -> throw new RedactionException("Unsupported redaction decision: null");
                case RedactionDecision.Remove ignored -> {
                    // no-op, header dropped
                }
                case RedactionDecision.Mask(String replacement) ->
                        redacted.put(headerName, maskedValues(normalizedValues, replacement));
                case RedactionDecision.Keep ignored ->
                        redacted.put(headerName, List.copyOf(normalizedValues));
            }
        }

        return Collections.unmodifiableMap(redacted);
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            normalized.add(value == null ? "" : value);
        }
        return normalized;
    }

    private List<String> maskedValues(List<String> values, String replacement) {
        if (values.isEmpty()) {
            return List.of();
        }
        List<String> masked = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            masked.add(replacement);
        }
        return List.copyOf(masked);
    }
}
