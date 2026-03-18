package dev.testloom.spring.capture;

import dev.testloom.core.capture.application.port.CaptureFailureHandler;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import lombok.extern.slf4j.Slf4j;

/**
 * Default failure handler that logs capture persistence failures.
 */
@Slf4j
public final class LoggingCaptureFailureHandler implements CaptureFailureHandler {
    @Override
    public void onCaptureFailure(CaptureEnvelope envelope, Exception exception) {
        String method = "UNKNOWN";
        String path = "UNKNOWN";
        if (envelope != null && envelope.request() != null) {
            if (envelope.request().method() != null) {
                method = envelope.request().method();
            }
            if (envelope.request().path() != null) {
                path = envelope.request().path();
            }
        }
        log.warn("Failed to persist Testloom capture for request [{} {}].", method, path, exception);
    }
}
