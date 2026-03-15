package dev.testloom.spring.capture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dev.testloom.spring.capture.model.CaptureEnvelope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Default {@link CaptureWriter} implementation that writes envelopes to local JSON files.
 */
public final class JsonFileCaptureWriter implements CaptureWriter {
    private static final String FILE_SUFFIX = ".json";

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
            Path target = directory.resolve(buildFileName(envelope));
            writer.writeValue(target.toFile(), envelope);
        } catch (IOException e) {
            throw new TestloomCaptureException("Failed to write capture envelope to disk.", e);
        }
    }

    private String buildFileName(CaptureEnvelope envelope) {
        String timestamp = envelope.recordedAt().replace(':', '-');
        String method = envelope.request().method().toLowerCase(Locale.ROOT);
        String path = sanitizePath(envelope.request().path());
        return timestamp + "_" + method + "_" + path + "_" + UUID.randomUUID() + FILE_SUFFIX;
    }

    private String sanitizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "root";
        }
        String sanitized = rawPath.replaceAll("[^a-zA-Z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "root" : sanitized;
    }
}
