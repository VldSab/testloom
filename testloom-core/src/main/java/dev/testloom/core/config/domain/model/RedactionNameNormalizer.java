package dev.testloom.core.config.domain.model;

import java.util.Locale;

/**
 * Shared normalization for redaction names and rule targets.
 */
public final class RedactionNameNormalizer {
    private RedactionNameNormalizer() {
    }

    public static String normalize(String value, RedactionTargetType targetType) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (targetType == RedactionTargetType.HEADER || targetType == RedactionTargetType.QUERY_PARAM) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed;
    }
}
