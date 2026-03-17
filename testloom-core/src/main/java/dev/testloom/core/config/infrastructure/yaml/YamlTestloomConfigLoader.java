package dev.testloom.core.config.infrastructure.yaml;

import dev.testloom.core.config.application.port.TestloomConfigLoader;
import dev.testloom.core.config.application.service.TestloomConfigNormalizer;
import dev.testloom.core.config.application.service.lint.TestloomLinter;
import dev.testloom.core.config.domain.exception.TestloomConfigException;
import dev.testloom.core.config.domain.model.TestloomConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * YAML-based implementation of {@link TestloomConfigLoader}.
 */
public final class YamlTestloomConfigLoader implements TestloomConfigLoader {
    private final ObjectMapper objectMapper;
    private final TestloomLinter linter;

    /**
     * Creates a loader configured for Testloom YAML parsing.
     */
    public YamlTestloomConfigLoader() {
        this(
                JsonMapper.builder(new YAMLFactory())
                        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                        .build(),
                TestloomLinter.defaultLinter()
        );
    }

    YamlTestloomConfigLoader(ObjectMapper objectMapper, TestloomLinter linter) {
        this.objectMapper = objectMapper;
        this.linter = linter;
    }

    /**
     * Loads config from YAML file and returns normalized validated result.
     *
     * @param path config file path
     * @return loaded config
     */
    @Override
    public TestloomConfig load(Path path) {
        if (path == null) {
            throw new TestloomConfigException("Config path must not be null.");
        }

        if (Files.notExists(path)) {
            throw new TestloomConfigException("Config file does not exist: " + path.toAbsolutePath());
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            TestloomConfigDocument document = objectMapper.readValue(inputStream, TestloomConfigDocument.class);
            if (document == null || document.getTestloom() == null) {
                throw new TestloomConfigException("Config file is empty or missing top-level 'testloom' key: " + path.toAbsolutePath());
            }
            TestloomConfig config = document.getTestloom();
            TestloomConfig normalized = TestloomConfigNormalizer.normalize(config);
            linter.lintOrThrow(normalized, path);
            return normalized;
        } catch (JsonProcessingException e) {
            throw new TestloomConfigException("Failed to parse YAML config at " + path.toAbsolutePath(), e);
        } catch (IOException e) {
            throw new TestloomConfigException("Failed to read YAML config at " + path.toAbsolutePath(), e);
        }
    }
}
