package dev.testloom.core.config.application.service;

import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.config.domain.model.TestloomConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Normalizes loaded config values into deterministic internal form.
 */
public final class TestloomConfigNormalizer {
    private TestloomConfigNormalizer() {
    }

    /**
     * Normalizes nullable and user-provided values.
     *
     * @param source loaded config
     * @return normalized config
     */
    public static TestloomConfig normalize(TestloomConfig source) {
        Objects.requireNonNull(source, "Config to normalize must not be null.");

        TestloomConfig normalized = new TestloomConfig();
        normalized.setRecorder(normalizeRecorder(source.getRecorder()));
        normalized.setRedaction(normalizeRedaction(source.getRedaction()));
        return normalized;
    }

    private static RecorderConfig normalizeRecorder(RecorderConfig source) {
        return mapIfNotNull(source, recorder -> {
            RecorderConfig normalized = new RecorderConfig();
            normalized.setEnabled(recorder.isEnabled());
            normalized.setMode(recorder.getMode());
            normalized.setOutputDir(trimToNull(recorder.getOutputDir()));
            normalized.setIncludeBodies(recorder.isIncludeBodies());
            normalized.setMaxBodySizeKb(recorder.getMaxBodySizeKb());
            normalized.setIncludePaths(normalizeDistinctStrings(recorder.getIncludePaths(), UnaryOperator.identity()));
            normalized.setExcludePaths(normalizeDistinctStrings(recorder.getExcludePaths(), UnaryOperator.identity()));
            return normalized;
        });
    }

    private static RedactionConfig normalizeRedaction(RedactionConfig source) {
        return mapIfNotNull(source, redaction -> {
            RedactionConfig normalized = new RedactionConfig();
            normalized.setMask(normalizeMask(redaction.getMask()));
            normalized.setHeaders(normalizeLowercaseList(redaction.getHeaders()));
            normalized.setJsonFields(normalizeDistinctStrings(redaction.getJsonFields(), UnaryOperator.identity()));
            normalized.setQueryParams(normalizeLowercaseList(redaction.getQueryParams()));
            normalized.setRules(normalizeRules(redaction.getRules()));
            return normalized;
        });
    }

    private static List<RedactionRule> normalizeRules(List<RedactionRule> rules) {
        if (isNullOrEmpty(rules)) {
            return List.of();
        }

        List<RedactionRule> normalized = new ArrayList<>();
        for (RedactionRule rule : rules) {
            normalized.add(normalizeRule(rule));
        }
        return Collections.unmodifiableList(normalized);
    }

    private static RedactionRule normalizeRule(RedactionRule source) {
        return mapIfNotNull(source, rule -> {
            RedactionRule normalized = new RedactionRule();
            normalized.setType(rule.getType());
            normalized.setMatcher(rule.getMatcher());
            normalized.setAction(rule.getAction());
            normalized.setTarget(normalizeRuleTarget(rule.getTarget(), normalized.getType()));
            normalized.setReplacement(trimToNull(rule.getReplacement()));
            return normalized;
        });
    }

    private static String normalizeRuleTarget(String value, RedactionTargetType targetType) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (targetType == RedactionTargetType.HEADER || targetType == RedactionTargetType.QUERY_PARAM) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private static String normalizeMask(String value) {
        return value == null ? null : value.trim();
    }

    private static List<String> normalizeLowercaseList(List<String> values) {
        return normalizeDistinctStrings(values, value -> value.toLowerCase(Locale.ROOT));
    }

    private static List<String> normalizeDistinctStrings(List<String> values, UnaryOperator<String> transformer) {
        Objects.requireNonNull(transformer, "transformer must not be null");
        if (isNullOrEmpty(values)) {
            return List.of();
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            normalized = transformer.apply(normalized);
            unique.add(normalized);
        }
        return List.copyOf(unique);
    }

    private static boolean isNullOrEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static <T, R> R mapIfNotNull(T value, Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return value == null ? null : mapper.apply(value);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
