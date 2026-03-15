package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests defensive behavior of {@link LoggingCaptureFailureHandler}.
 */
class LoggingCaptureFailureHandlerTest {
    private final LoggingCaptureFailureHandler handler = new LoggingCaptureFailureHandler();

    @Test
    void handlesNullEnvelope() {
        assertDoesNotThrow(() -> handler.onCaptureFailure(null, new RuntimeException("boom")));
    }

    @Test
    void handlesEnvelopeWithNullRequest() {
        CaptureEnvelope envelope = new CaptureEnvelope("0.1.0", "2026-03-15T12:00:00Z", "HTTP", null, null);

        assertDoesNotThrow(() -> handler.onCaptureFailure(envelope, new RuntimeException("boom")));
    }

    @Test
    void handlesEnvelopeWithNullMethodAndPath() {
        CaptureEnvelope envelope = new CaptureEnvelope(
                "0.1.0",
                "2026-03-15T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(null, null, null, Map.of(), null, null),
                new CaptureEnvelope.ResponseCapture(200, Map.of(), null, null, 1)
        );

        assertDoesNotThrow(() -> handler.onCaptureFailure(envelope, new RuntimeException("boom")));
    }

    @Test
    void handlesEnvelopeWithMethodAndPath() {
        CaptureEnvelope envelope = new CaptureEnvelope(
                "0.1.0",
                "2026-03-15T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture("GET", "/api/hello", null, Map.of("accept", List.of("*/*")), null, null),
                new CaptureEnvelope.ResponseCapture(200, Map.of(), null, null, 1)
        );

        assertDoesNotThrow(() -> handler.onCaptureFailure(envelope, new RuntimeException("boom")));
    }
}
