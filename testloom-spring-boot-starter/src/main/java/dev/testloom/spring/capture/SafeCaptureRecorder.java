package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;

import java.util.Objects;

/**
 * Safe capture recorder that prevents sink failures from leaking to request handling.
 */
public final class SafeCaptureRecorder implements CaptureRecorder {
    private final CaptureWriter captureWriter;
    private final CaptureFailureHandler failureHandler;

    /**
     * Creates a safe recorder.
     *
     * @param captureWriter destination writer
     * @param failureHandler failure callback
     */
    public SafeCaptureRecorder(CaptureWriter captureWriter, CaptureFailureHandler failureHandler) {
        this.captureWriter = Objects.requireNonNull(captureWriter, "captureWriter must not be null");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler must not be null");
    }

    @Override
    public void record(CaptureEnvelope envelope) {
        try {
            captureWriter.write(envelope);
        } catch (Exception exception) {
            failureHandler.onCaptureFailure(envelope, exception);
        }
    }
}
