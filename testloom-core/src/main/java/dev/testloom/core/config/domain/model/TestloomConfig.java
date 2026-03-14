package dev.testloom.core.config.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Root domain configuration aggregate for Testloom.
 */
@Getter
@Setter
public class TestloomConfig {
    private RecorderConfig recorder;
    private RedactionConfig redaction;

    /**
     * Returns default application config.
     *
     * @return default config
     */
    public static TestloomConfig defaults() {
        TestloomConfig config = new TestloomConfig();
        config.setRecorder(RecorderConfig.defaults());
        config.setRedaction(RedactionConfig.defaults());
        return config;
    }
}
