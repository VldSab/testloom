package dev.testloom.core.redaction.application.port;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests shared CaptureRedactor contract helpers.
 */
class CaptureRedactorTest {
    @Test
    void noOpReturnsSingletonAndIdentityBehavior() {
        CaptureRedactor first = CaptureRedactor.noOp();
        CaptureRedactor second = CaptureRedactor.noOp();

        assertThat(first).isSameInstanceAs(second);

        CaptureEnvelope envelope = sampleEnvelope();
        assertThat(first.redact(envelope)).isSameInstanceAs(envelope);
    }

    private static CaptureEnvelope sampleEnvelope() {
        return new CaptureEnvelope(
                "0.1.0",
                "2026-03-17T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        "GET",
                        "/api/hello",
                        null,
                        Map.of("accept", List.of("application/json")),
                        null,
                        null,
                        new CaptureEnvelope.Truncation(false, 0, 0)
                ),
                new CaptureEnvelope.ResponseCapture(
                        200,
                        Map.of(),
                        "{\"message\":\"hello\"}",
                        "application/json",
                        5,
                        new CaptureEnvelope.Truncation(false, 19, 19)
                )
        );
    }
}
