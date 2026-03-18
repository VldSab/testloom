package dev.testloom.core.redaction.application.service.part;

/**
 * One request-part redaction step in ordered redaction pipeline.
 */
@FunctionalInterface
public interface RequestPartRedactor {
    void apply(RequestDraft draft);
}
