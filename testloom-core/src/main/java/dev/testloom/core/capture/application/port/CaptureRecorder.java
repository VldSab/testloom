package dev.testloom.core.capture.application.port;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;

/**
 * High-level contract for recording one capture envelope.
 */
public interface CaptureRecorder {
    /**
     * Records the envelope using implementation-specific policy.
     *
     * @param envelope capture envelope
     */
    void record(CaptureEnvelope envelope);
}
