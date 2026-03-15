package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default failure handler that logs capture persistence failures.
 */
public final class LoggingCaptureFailureHandler implements CaptureFailureHandler {
    private static final Log log = LogFactory.getLog(LoggingCaptureFailureHandler.class);

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
        log.warn("Failed to persist Testloom capture for request [" + method + " " + path + "].", exception);
    }
}
