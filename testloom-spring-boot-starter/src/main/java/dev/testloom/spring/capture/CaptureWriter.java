package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;

/**
 * Writes captured transport envelopes to a concrete destination.
 */
public interface CaptureWriter {
    /**
     * Persists a single capture envelope.
     *
     * @param envelope normalized transport capture payload
     */
    void write(CaptureEnvelope envelope);
}
