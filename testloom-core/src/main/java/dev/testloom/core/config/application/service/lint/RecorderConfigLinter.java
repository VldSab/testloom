package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.model.RecorderConfig;

import java.util.List;

/**
 * Lints recorder-specific config values.
 */
public final class RecorderConfigLinter implements ConfigSectionLinter<RecorderConfig> {
    @Override
    public void lint(RecorderConfig recorder, List<String> errors) {
        if (recorder == null) {
            errors.add("testloom.recorder must not be null.");
            return;
        }
        if (recorder.getMode() == null) {
            errors.add("testloom.recorder.mode must be one of LOCAL, DEV, STAGING.");
        }
        if (LintStrings.isBlank(recorder.getOutputDir())) {
            errors.add("testloom.recorder.output-dir must not be blank.");
        }
        if (recorder.getMaxBodySizeKb() <= 0) {
            errors.add("testloom.recorder.max-body-size-kb must be > 0.");
        }
    }
}
