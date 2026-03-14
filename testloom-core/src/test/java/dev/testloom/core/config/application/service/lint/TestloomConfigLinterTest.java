package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.exception.TestloomConfigValidationException;
import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests orchestration behavior of the top-level config linter.
 */
class TestloomConfigLinterTest {
    @Test
    void constructorRejectsNullSectionLinterList() {
        assertThrows(NullPointerException.class, () -> new TestloomConfigLinter(null));
    }

    @Test
    void lintReturnsErrorWhenConfigIsNull() {
        TestloomConfigLinter linter = new TestloomConfigLinter(List.of((config, errors) -> {
        }));

        List<String> errors = linter.lint(null);

        assertThat(errors).containsExactly("Configuration must not be null.");
    }

    @Test
    void lintOrThrowRejectsNullSourcePath() {
        TestloomConfigLinter linter = new TestloomConfigLinter(List.of((config, errors) -> errors.add("boom")));

        assertThrows(NullPointerException.class, () -> linter.lintOrThrow(new TestloomConfig(), null));
    }

    @Test
    void lintOrThrowIncludesAbsolutePathAndMessages(@TempDir Path tempDir) {
        Path source = tempDir.resolve("testloom.yaml");
        TestloomConfigLinter linter = new TestloomConfigLinter(List.of(
                (config, errors) -> errors.add("first"),
                (config, errors) -> errors.add("second")
        ));

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> linter.lintOrThrow(new TestloomConfig(), source)
        );

        assertThat(error).hasMessageThat().contains(source.toAbsolutePath().toString());
        assertThat(error).hasMessageThat().contains("first");
        assertThat(error).hasMessageThat().contains("second");
    }

    @Test
    void lintOrThrowDoesNothingWhenNoErrors(@TempDir Path tempDir) {
        Path source = tempDir.resolve("testloom.yaml");
        TestloomConfigLinter linter = new TestloomConfigLinter(List.of((config, errors) -> {
        }));

        linter.lintOrThrow(new TestloomConfig(), source);
    }

    @Test
    void defaultLinterValidatesBothSections(@TempDir Path tempDir) {
        Path source = tempDir.resolve("testloom.yaml");
        TestloomConfig config = new TestloomConfig();

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> TestloomConfigLinter.defaultLinter().lintOrThrow(config, source)
        );

        assertThat(error).hasMessageThat().contains("testloom.recorder must not be null.");
        assertThat(error).hasMessageThat().contains("testloom.redaction must not be null.");
    }

    @Test
    void lintAggregatesErrorsFromAllSectionLinters() {
        List<String> trace = new ArrayList<>();
        TestloomConfigLinter linter = new TestloomConfigLinter(List.of(
                (config, errors) -> {
                    trace.add("first");
                    errors.add("a");
                },
                (config, errors) -> {
                    trace.add("second");
                    errors.add("b");
                }
        ));

        List<String> errors = linter.lint(new TestloomConfig());

        assertThat(trace).containsExactly("first", "second").inOrder();
        assertThat(errors).containsExactly("a", "b").inOrder();
    }
}
