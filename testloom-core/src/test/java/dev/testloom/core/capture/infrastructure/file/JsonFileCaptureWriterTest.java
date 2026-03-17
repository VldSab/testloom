package dev.testloom.core.capture.infrastructure.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.core.capture.domain.exception.TestloomCaptureException;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests file-system behavior for {@link JsonFileCaptureWriter}.
 */
class JsonFileCaptureWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writeCreatesDirectoryAndJsonFile(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures").resolve("nested");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());

        writer.write(sampleEnvelope("/api/hello"));

        List<Path> files = listFiles(outputDir);
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().getFileName().toString()).isEqualTo("20260315T120000000Z_api_hello.json");

        JsonNode root = objectMapper.readTree(files.getFirst().toFile());
        assertThat(root.get("schemaVersion").asText()).isEqualTo("0.1.0");
        assertThat(root.get("transport").asText()).isEqualTo("HTTP");
        assertThat(root.get("request").get("method").asText()).isEqualTo("GET");
        assertThat(root.get("request").get("path").asText()).isEqualTo("/api/hello");
        assertThat(root.get("response").get("status").asInt()).isEqualTo(200);
    }

    @Test
    void writeUsesRootSegmentWhenPathIsBlank(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());

        writer.write(sampleEnvelope("   "));

        String fileName = listFiles(outputDir).getFirst().getFileName().toString();
        assertThat(fileName).isEqualTo("20260315T120000000Z_root.json");
    }

    @Test
    void writeSanitizesPathForFilename(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());

        writer.write(sampleEnvelope("/v1/orders/{id}"));

        String fileName = listFiles(outputDir).getFirst().getFileName().toString();
        assertThat(fileName).isEqualTo("20260315T120000000Z_v1_orders_id.json");
    }

    @Test
    void writeAppendsNumericSuffixWhenNameCollides(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());
        CaptureEnvelope envelope = sampleEnvelope("/api/hello");

        writer.write(envelope);
        writer.write(envelope);

        List<Path> files = listFiles(outputDir);
        assertThat(files).hasSize(2);
        assertThat(files.get(0).getFileName().toString()).isEqualTo("20260315T120000000Z_api_hello.json");
        assertThat(files.get(1).getFileName().toString()).isEqualTo("20260315T120000000Z_api_hello_2.json");
    }

    @Test
    void writeUsesCurrentTimestampWhenRecordedAtIsBlank(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());
        CaptureEnvelope envelope = new CaptureEnvelope(
                "0.1.0",
                "   ",
                "HTTP",
                sampleEnvelope("/api/hello").request(),
                sampleEnvelope("/api/hello").response()
        );

        writer.write(envelope);

        String fileName = listFiles(outputDir).getFirst().getFileName().toString();
        assertThat(fileName).matches("\\d{8}T\\d{9}Z_api_hello\\.json");
    }

    @Test
    void writeSanitizesInvalidTimestampAndNullRequestPath(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());
        CaptureEnvelope envelope = new CaptureEnvelope(
                "0.1.0",
                "bad/time",
                "HTTP",
                null,
                sampleEnvelope("/api/hello").response()
        );

        writer.write(envelope);

        String fileName = listFiles(outputDir).getFirst().getFileName().toString();
        assertThat(fileName).isEqualTo("bad_time_root.json");
    }

    @Test
    void writeUsesRootFallbackWhenPathSanitizesToEmpty(@TempDir Path tempDir) throws Exception {
        Path outputDir = tempDir.resolve("captures");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputDir.toString());

        writer.write(sampleEnvelope("////"));

        String fileName = listFiles(outputDir).getFirst().getFileName().toString();
        assertThat(fileName).isEqualTo("20260315T120000000Z_root.json");
    }

    @Test
    void writeThrowsTestloomCaptureExceptionWhenOutputPathIsRegularFile(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("not-a-dir");
        Files.writeString(outputFile, "x");
        JsonFileCaptureWriter writer = new JsonFileCaptureWriter(objectMapper, outputFile.toString());

        TestloomCaptureException error = assertThrows(
                TestloomCaptureException.class,
                () -> writer.write(sampleEnvelope("/api/hello"))
        );

        assertThat(error).hasMessageThat().contains("Failed to write capture envelope to disk");
        assertThat(error.getCause()).isNotNull();
    }

    @Test
    void constructorRejectsNullOutputDirectory() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> new JsonFileCaptureWriter(objectMapper, null)
        );

        assertThat(error).hasMessageThat().contains("outputDirectory must not be null");
    }

    @Test
    void constructorRejectsNullObjectMapper() {
        assertThrows(NullPointerException.class, () -> new JsonFileCaptureWriter(null, "captures"));
    }

    private static List<Path> listFiles(Path directory) throws Exception {
        try (var stream = Files.list(directory)) {
            return stream.sorted().toList();
        }
    }

    private static CaptureEnvelope sampleEnvelope(String path) {
        return new CaptureEnvelope(
                "0.1.0",
                "2026-03-15T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        "GET",
                        path,
                        "q=1",
                        Map.of("accept", List.of("application/json")),
                        null,
                        "application/json",
                        new CaptureEnvelope.Truncation(false, 0, 0)
                ),
                new CaptureEnvelope.ResponseCapture(
                        200,
                        Map.of("content-type", List.of("application/json")),
                        "{\"message\":\"hello\"}",
                        "application/json",
                        10,
                        new CaptureEnvelope.Truncation(false, 19, 19)
                )
        );
    }
}
