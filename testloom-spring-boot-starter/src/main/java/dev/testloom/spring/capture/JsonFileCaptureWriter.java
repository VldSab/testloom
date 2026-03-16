package dev.testloom.spring.capture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dev.testloom.spring.capture.model.CaptureEnvelope;

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
 * Default {@link CaptureWriter} implementation that writes envelopes to local JSON files.
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
     * Creates a JSON file writer.
     *
     * @param objectMapper mapper used to serialize envelopes
     * @param outputDirectory directory where capture files are written
     */
    public JsonFileCaptureWriter(ObjectMapper objectMapper, String outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        this.writer = objectMapper.writerWithDefaultPrettyPrinter();
    }

    /**
     * Writes one envelope to the configured output directory.
     *
     * <p>The output directory is created when it does not exist.
     *
     * @param envelope envelope to write
     * @throws TestloomCaptureException when the file cannot be written
     */
    @Override
    public void write(CaptureEnvelope envelope) {
        try {
            Path directory = Path.of(outputDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String baseFileName = buildBaseFileName(envelope);
            Path target = resolveTargetPath(directory, baseFileName);
            writer.writeValue(target.toFile(), envelope);
        } catch (IOException e) {
            throw new TestloomCaptureException("Failed to write capture envelope to disk.", e);
        }
    }

    private String buildBaseFileName(CaptureEnvelope envelope) {
        String timestamp = normalizeTimestamp(envelope.recordedAt());
        String route = sanitizeRoute(envelope.request().path());
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
