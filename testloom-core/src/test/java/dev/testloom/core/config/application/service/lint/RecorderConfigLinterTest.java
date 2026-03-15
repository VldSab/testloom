package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.RecorderMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests recorder section linting rules.
 */
class RecorderConfigLinterTest {
    private final RecorderConfigLinter linter = new RecorderConfigLinter();

    @Test
    void nullRecorderProducesError() {
        List<String> errors = new ArrayList<>();

        linter.lint(null, errors);

        assertThat(errors).containsExactly("testloom.recorder must not be null.");
    }

    @Test
    void invalidRecorderProducesAllExpectedErrors() {
        RecorderConfig recorder = new RecorderConfig();
        recorder.setMode(null);
        recorder.setOutputDir("   ");
        recorder.setMaxBodySizeBytes(0);

        List<String> errors = new ArrayList<>();
        linter.lint(recorder, errors);

        assertThat(errors).containsAtLeast(
                "testloom.recorder.mode must be one of LOCAL, DEV, STAGING.",
                "testloom.recorder.output-dir must not be blank.",
                "testloom.recorder.max-body-size-bytes must be > 0."
        );
        assertThat(errors).hasSize(3);
    }

    @Test
    void validRecorderProducesNoErrors() {
        RecorderConfig recorder = new RecorderConfig();
        recorder.setMode(RecorderMode.LOCAL);
        recorder.setOutputDir("./.testloom/captures");
        recorder.setMaxBodySizeBytes(65536);

        List<String> errors = new ArrayList<>();
        linter.lint(recorder, errors);

        assertThat(errors).isEmpty();
    }
}
