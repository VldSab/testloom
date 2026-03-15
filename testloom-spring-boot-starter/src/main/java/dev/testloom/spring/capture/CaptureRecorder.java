package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;

/**
 * Records captured envelopes with implementation-specific safety and failure policy.
 */
public interface CaptureRecorder {
    /**
     * Records one captured envelope.
     *
     * @param envelope capture envelope to record
     */
    void record(CaptureEnvelope envelope);
}
