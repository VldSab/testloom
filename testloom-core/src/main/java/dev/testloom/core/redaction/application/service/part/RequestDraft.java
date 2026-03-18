package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Mutable request draft used by request redaction pipeline.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class RequestDraft {
    private final String method;
    private final String path;
    private final String contentType;
    private final CaptureEnvelope.Truncation truncation;
    private String query;
    private Map<String, List<String>> headers;
    private String body;

    public RequestDraft(CaptureEnvelope.RequestCapture source) {
        this.method = source.method();
        this.path = source.path();
        this.query = source.query();
        this.headers = source.headers();
        this.body = source.body();
        this.contentType = source.contentType();
        this.truncation = source.truncation();
    }

    public CaptureEnvelope.RequestCapture toCapture() {
        return new CaptureEnvelope.RequestCapture(
                method,
                path,
                query,
                headers,
                body,
                contentType,
                truncation
        );
    }
}
