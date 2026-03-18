package dev.testloom.core.capture.application.service;

import dev.testloom.core.capture.application.port.CaptureFailureHandler;
import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.application.port.CaptureWriter;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Fail-safe recorder that never propagates writer failures to caller code.
 */
@Slf4j
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
            log.debug("Capture writer failure delegated to failure handler.", exception);
            failureHandler.onCaptureFailure(envelope, exception);
        }
    }
}
