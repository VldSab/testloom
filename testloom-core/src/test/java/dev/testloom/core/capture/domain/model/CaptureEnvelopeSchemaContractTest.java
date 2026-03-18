package dev.testloom.core.capture.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies stable JSON field contract for capture schema v0.1.0.
 */
class CaptureEnvelopeSchemaContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void envelopeSerializesWithExpectedFieldNames() {
        CaptureEnvelope envelope = sampleEnvelope();

        JsonNode root = objectMapper.valueToTree(envelope);
        assertThat(fieldNames(root))
                .containsExactly("schemaVersion", "recordedAt", "transport", "request", "response")
                .inOrder();
        assertThat(fieldNames(root.get("request")))
                .containsExactly("method", "path", "query", "headers", "body", "contentType", "truncation")
                .inOrder();
        assertThat(fieldNames(root.get("response")))
                .containsExactly("status", "headers", "body", "contentType", "durationMs", "truncation")
                .inOrder();
        assertThat(fieldNames(root.get("request").get("truncation")))
                .containsExactly("bodyTruncated", "originalSizeBytes", "capturedSizeBytes")
                .inOrder();
        assertThat(fieldNames(root.get("response").get("truncation")))
                .containsExactly("bodyTruncated", "originalSizeBytes", "capturedSizeBytes")
                .inOrder();
    }

    private static Set<String> fieldNames(JsonNode node) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static CaptureEnvelope sampleEnvelope() {
        return new CaptureEnvelope(
                "0.1.0",
                "2026-03-17T10:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        "POST",
                        "/api/orders",
                        "expand=true",
                        Map.of("accept", List.of("application/json")),
                        "{\"name\":\"demo\"}",
                        "application/json",
                        new CaptureEnvelope.Truncation(false, 15, 15)
                ),
                new CaptureEnvelope.ResponseCapture(
                        201,
                        Map.of("content-type", List.of("application/json")),
                        "{\"id\":1}",
                        "application/json",
                        12,
                        new CaptureEnvelope.Truncation(false, 8, 8)
                )
        );
    }
}
