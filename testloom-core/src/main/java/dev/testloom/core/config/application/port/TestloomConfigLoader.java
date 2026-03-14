package dev.testloom.core.config.application.port;

import dev.testloom.core.config.domain.model.TestloomConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads Testloom configuration from an external source.
 */
public interface TestloomConfigLoader {
    /**
     * Loads and returns normalized configuration.
     *
     * @param path path to config file
     * @return loaded config
     */
    TestloomConfig load(Path path);

    /**
     * Loads config from file when present.
     *
     * @param path path to config file
     * @return loaded config wrapped in optional, empty when file is absent
     */
    default Optional<TestloomConfig> loadIfExists(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Config path must not be null.");
        }
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        return Optional.of(load(path));
    }
}
