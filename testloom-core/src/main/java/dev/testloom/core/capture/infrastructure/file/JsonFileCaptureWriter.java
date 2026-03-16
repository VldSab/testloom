package dev.testloom.core.capture.infrastructure.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dev.testloom.core.capture.application.port.CaptureWriter;
import dev.testloom.core.capture.domain.exception.TestloomCaptureException;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * File-system writer that persists capture envelopes as JSON files.
 *
 * <p>File naming contract is {@code <timestamp>_<route>.json}. If a file with the same
 * name already exists, a deterministic numeric suffix is appended:
 * {@code <timestamp>_<route>_2.json}, {@code ..._3.json}, and so on.
 */
public final class JsonFileCaptureWriter implements CaptureWriter {
    private static final String FILE_SUFFIX = ".json";
    private static final Pattern NON_FILE_SAFE = Pattern.compile("[^a-zA-Z0-9_-]+");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC);

    private final String outputDirectory;
    private final ObjectWriter writer;

    /**
     * Creates a JSON file capture writer.
     *
     * @param objectMapper mapper used for envelope serialization
     * @param outputDirectory target directory for capture files
     */
    public JsonFileCaptureWriter(ObjectMapper objectMapper, String outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        this.writer = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .writerWithDefaultPrettyPrinter();
    }

    @Override
    public void write(CaptureEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        try {
            Path directory = Path.of(outputDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String baseFileName = buildBaseFileName(envelope);
            Path target = resolveTargetPath(directory, baseFileName);
            writer.writeValue(target.toFile(), envelope);
        } catch (IOException exception) {
            throw new TestloomCaptureException("Failed to write capture envelope to disk.", exception);
        }
    }

    private String buildBaseFileName(CaptureEnvelope envelope) {
        String timestamp = normalizeTimestamp(envelope.recordedAt());
        String route = sanitizeRoute(envelope.request() == null ? null : envelope.request().path());
        return timestamp + "_" + route;
    }

    private Path resolveTargetPath(Path directory, String baseFileName) {
        Path candidate = directory.resolve(baseFileName + FILE_SUFFIX);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseFileName + "_" + suffix + FILE_SUFFIX);
            suffix++;
        }
        return candidate;
    }

    private String normalizeTimestamp(String recordedAt) {
        if (recordedAt == null || recordedAt.isBlank()) {
            return FILE_TIMESTAMP_FORMAT.format(Instant.now());
        }
        try {
            return FILE_TIMESTAMP_FORMAT.format(Instant.parse(recordedAt));
        } catch (DateTimeParseException ignored) {
            return sanitizeSegment(recordedAt, "time");
        }
    }

    private String sanitizeRoute(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "root";
        }
        return sanitizeSegment(rawPath, "root");
    }

    private String sanitizeSegment(String rawValue, String fallback) {
        String sanitized = NON_FILE_SAFE.matcher(rawValue).replaceAll("_")
                .replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
