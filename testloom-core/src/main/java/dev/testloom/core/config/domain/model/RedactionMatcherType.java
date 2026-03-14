package dev.testloom.core.config.domain.model;

/**
 * Matcher strategy used to evaluate redaction rule targets.
 */
public enum RedactionMatcherType {
    EXACT,
    GLOB,
    REGEX
}
