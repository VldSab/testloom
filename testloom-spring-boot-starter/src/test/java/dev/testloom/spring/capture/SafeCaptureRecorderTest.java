package dev.testloom.spring.capture;

import dev.testloom.spring.capture.model.CaptureEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests failure handling behavior of {@link SafeCaptureRecorder}.
 */
class SafeCaptureRecorderTest {
    @Test
    void recordWritesEnvelopeWhenWriterSucceeds() {
        AtomicReference<CaptureEnvelope> capturedEnvelope = new AtomicReference<>();
        CaptureWriter writer = capturedEnvelope::set;

        AtomicBoolean failureCalled = new AtomicBoolean(false);
        CaptureFailureHandler handler = (envelope, exception) -> failureCalled.set(true);

        SafeCaptureRecorder recorder = new SafeCaptureRecorder(writer, handler);
        CaptureEnvelope envelope = sampleEnvelope();

        recorder.record(envelope);

        assertThat(capturedEnvelope.get()).isSameInstanceAs(envelope);
        assertThat(failureCalled.get()).isFalse();
    }

    @Test
    void recordSwallowsWriterFailureAndDelegatesToFailureHandler() {
        Exception writerError = new Exception("boom");
        CaptureWriter failingWriter = envelope -> {
            throw new RuntimeException(writerError);
        };

        AtomicReference<Exception> capturedError = new AtomicReference<>();
        CaptureFailureHandler handler = (envelope, exception) -> capturedError.set(exception);

        SafeCaptureRecorder recorder = new SafeCaptureRecorder(failingWriter, handler);
        recorder.record(sampleEnvelope());

        assertThat(capturedError.get()).isInstanceOf(RuntimeException.class);
        assertThat(capturedError.get().getCause()).isSameInstanceAs(writerError);
    }

    @Test
    void constructorRejectsNullWriter() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> new SafeCaptureRecorder(null, (envelope, exception) -> { })
        );

        assertThat(error).hasMessageThat().contains("captureWriter must not be null");
    }

    @Test
    void constructorRejectsNullFailureHandler() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> new SafeCaptureRecorder(envelope -> { }, null)
        );

        assertThat(error).hasMessageThat().contains("failureHandler must not be null");
    }

    private static CaptureEnvelope sampleEnvelope() {
        return new CaptureEnvelope(
                "0.1.0",
                "2026-03-15T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        "GET",
                        "/api/hello",
                        null,
                        Map.of("accept", List.of("application/json")),
                        null,
                        null
                ),
                new CaptureEnvelope.ResponseCapture(
                        200,
                        Map.of(),
                        "{\"message\":\"hello\"}",
                        "application/json",
                        5
                )
        );
    }
}
