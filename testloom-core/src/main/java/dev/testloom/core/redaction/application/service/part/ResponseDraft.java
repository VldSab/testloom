package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Mutable response draft used by response redaction pipeline.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ResponseDraft {
    private final int status;
    private final String contentType;
    private final long durationMs;
    private final CaptureEnvelope.Truncation truncation;
    private Map<String, List<String>> headers;
    private String body;

    public ResponseDraft(CaptureEnvelope.ResponseCapture source) {
        this.status = source.status();
        this.headers = source.headers();
        this.body = source.body();
        this.contentType = source.contentType();
        this.durationMs = source.durationMs();
        this.truncation = source.truncation();
    }

    public CaptureEnvelope.ResponseCapture toCapture() {
        return new CaptureEnvelope.ResponseCapture(
                status,
                headers,
                body,
                contentType,
                durationMs,
                truncation
        );
    }
}
