package dev.testloom.core.capture.application.port;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;

/**
 * Handles persistence failures that happen during capture recording.
 */
public interface CaptureFailureHandler {
    /**
     * Handles capture persistence failure.
     *
     * @param envelope envelope that failed to persist
     * @param exception failure thrown by writer infrastructure
     */
    void onCaptureFailure(CaptureEnvelope envelope, Exception exception);
}
