package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Redacts raw query string using compiled policy.
 *
 * <p>Contract:
 * <ul>
 *     <li>Redaction is done on raw query string without URL decoding.</li>
 *     <li>{@code split("&", -1)} is used to preserve empty trailing segments.</li>
 *     <li>For flag-style params without value (for example {@code token}),
 *     mask action preserves original syntax and does not append {@code =mask}.</li>
 * </ul>
 */
public final class QueryCaptureRedactor {
    private final RedactionPolicy policy;

    public QueryCaptureRedactor(RedactionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public String redact(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        String[] segments = query.split("&", -1);
        List<String> redacted = new ArrayList<>(segments.length);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                redacted.add(segment);
                continue;
            }

            int equalsIndex = segment.indexOf('=');
            String rawName = equalsIndex >= 0 ? segment.substring(0, equalsIndex) : segment;

            RedactionDecision decision = policy.resolve(RedactionTargetType.QUERY_PARAM, rawName);
            switch (decision) {
                case null -> throw new RedactionException("Unsupported redaction decision: null");
                case RedactionDecision.Remove ignored -> {
                    // no-op, param dropped
                }
                case RedactionDecision.Mask(String replacement) ->
                        redacted.add(maskedSegment(rawName, equalsIndex, replacement));
                case RedactionDecision.Keep ignored -> redacted.add(segment);
            }
        }

        if (redacted.isEmpty()) {
            return null;
        }
        return String.join("&", redacted);
    }

    private String maskedSegment(String rawName, int equalsIndex, String replacement) {
        if (equalsIndex < 0) {
            return rawName;
        }
        return rawName + "=" + replacement;
    }
}
