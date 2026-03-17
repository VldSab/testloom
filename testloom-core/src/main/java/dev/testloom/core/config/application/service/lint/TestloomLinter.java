package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.exception.TestloomConfigValidationException;
import dev.testloom.core.config.domain.model.TestloomConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main linter that orchestrates all section-specific linters.
 */
public final class TestloomLinter {
    private static final TestloomLinter DEFAULT_LINTER = createDefault();
    private final List<ConfigSectionLinter<TestloomConfig>> sectionLinters;

    public TestloomLinter(List<ConfigSectionLinter<TestloomConfig>> sectionLinters) {
        this.sectionLinters = List.copyOf(Objects.requireNonNull(sectionLinters, "sectionLinters must not be null"));
    }

    /**
     * Returns the default linter composition used by runtime loaders.
     *
     * @return default linter
     */
    public static TestloomLinter defaultLinter() {
        return DEFAULT_LINTER;
    }

    /**
     * Lints config and throws if lint errors are found.
     *
     * @param config config to lint
     * @param source source identifier used in lint error messages
     */
    public void lintOrThrow(TestloomConfig config, String source) {
        Objects.requireNonNull(source, "source must not be null");
        List<String> errors = lint(config);
        if (!errors.isEmpty()) {
            throw new TestloomConfigValidationException(
                    "Invalid testloom config at " + source + ": " + String.join("; ", errors)
            );
        }
    }

    /**
     * Lints config and throws if lint errors are found.
     *
     * @param config config to lint
     * @param source source path used in lint error messages
     */
    public void lintOrThrow(TestloomConfig config, Path source) {
        lintOrThrow(config, Objects.requireNonNull(source, "source must not be null").toAbsolutePath().toString());
    }

    List<String> lint(TestloomConfig config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            errors.add("Configuration must not be null.");
            return errors;
        }

        for (ConfigSectionLinter<TestloomConfig> sectionLinter : sectionLinters) {
            sectionLinter.lint(config, errors);
        }
        return errors;
    }

    private static TestloomLinter createDefault() {
        RecorderConfigLinter recorderConfigLinter = new RecorderConfigLinter();
        RedactionConfigLinter redactionConfigLinter = new RedactionConfigLinter();

        return new TestloomLinter(List.of(
                (config, errors) -> recorderConfigLinter.lint(config.getRecorder(), errors),
                (config, errors) -> redactionConfigLinter.lint(config.getRedaction(), errors)
        ));
    }
}
