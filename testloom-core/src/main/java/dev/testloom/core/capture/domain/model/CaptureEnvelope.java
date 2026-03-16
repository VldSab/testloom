package dev.testloom.core.capture.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Transport-neutral capture payload written by runtime adapters.
 *
 * @param schemaVersion capture schema version
 * @param recordedAt ISO-8601 timestamp of capture creation
 * @param transport transport identifier, for example {@code HTTP}
 * @param request captured request details
 * @param response captured response details
 */
public record CaptureEnvelope(
        String schemaVersion,
        String recordedAt,
        String transport,
        RequestCapture request,
        ResponseCapture response
) {
    /**
     * Captured request payload.
     *
     * @param method transport method, for example HTTP method
     * @param path request path
     * @param query raw query string
     * @param headers request headers
     * @param body request body, may be {@code null}
     * @param contentType request content type
     */
    public record RequestCapture(
            String method,
            String path,
            String query,
            Map<String, List<String>> headers,
            String body,
            String contentType
    ) {
    }

    /**
     * Captured response payload.
     *
     * @param status transport status code
     * @param headers response headers
     * @param body response body, may be {@code null}
     * @param contentType response content type
     * @param durationMs request processing duration in milliseconds
     */
    public record ResponseCapture(
            int status,
            Map<String, List<String>> headers,
            String body,
            String contentType,
            long durationMs
    ) {
    }
}
