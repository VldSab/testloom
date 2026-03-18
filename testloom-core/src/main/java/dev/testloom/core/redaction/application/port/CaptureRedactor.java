package dev.testloom.core.redaction.application.port;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;

/**
 * Sanitizes captured payloads before persistence.
 */
public interface CaptureRedactor {
    /**
     * Returns a redacted copy of the input envelope.
     *
     * @param envelope raw captured envelope
     * @return sanitized envelope ready for persistence
     */
    CaptureEnvelope redact(CaptureEnvelope envelope);

    /**
     * Returns a no-op redactor.
     */
    static CaptureRedactor noOp() {
        return envelope -> envelope;
    }
}
