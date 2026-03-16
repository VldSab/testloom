package dev.testloom.core.capture.application.port;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;

/**
 * Writes captured envelopes to a concrete destination.
 */
public interface CaptureWriter {
    /**
     * Persists a single capture envelope.
     *
     * @param envelope capture envelope
     */
    void write(CaptureEnvelope envelope);
}
