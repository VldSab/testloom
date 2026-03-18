package dev.testloom.core.redaction.application.service.part;

/**
 * One response-part redaction step in ordered redaction pipeline.
 */
@FunctionalInterface
public interface ResponsePartRedactor {
    void apply(ResponseDraft draft);
}
