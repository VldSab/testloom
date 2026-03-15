package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;

/**
 * Handles failures that occur while persisting captured envelopes.
 */
public interface CaptureFailureHandler {
    /**
     * Handles a capture write failure.
     *
     * @param envelope capture envelope that failed to persist
     * @param exception failure thrown by recorder infrastructure
     */
    void onCaptureFailure(CaptureEnvelope envelope, Exception exception);
}
