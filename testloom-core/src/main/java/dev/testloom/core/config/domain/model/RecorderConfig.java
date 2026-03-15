package dev.testloom.core.config.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Capture recorder settings loaded from configuration.
 */
@Getter
@Setter
public class RecorderConfig {
    private boolean enabled;
    private RecorderMode mode;
    @JsonProperty("output-dir")
    private String outputDir;
    @JsonProperty("include-bodies")
    private boolean includeBodies;
    @JsonProperty("max-body-size-bytes")
    private int maxBodySizeBytes;
    @JsonProperty("include-paths")
    private List<String> includePaths;
    @JsonProperty("exclude-paths")
    private List<String> excludePaths;

    /**
     * Returns default recorder settings.
     *
     * @return default recorder config
     */
    public static RecorderConfig defaults() {
        RecorderConfig config = new RecorderConfig();
        config.setEnabled(RecorderDefaults.ENABLED);
        config.setMode(RecorderDefaults.MODE);
        config.setOutputDir(RecorderDefaults.OUTPUT_DIR);
        config.setIncludeBodies(RecorderDefaults.INCLUDE_BODIES);
        config.setMaxBodySizeBytes(RecorderDefaults.MAX_BODY_SIZE_BYTES);
        config.setIncludePaths(RecorderDefaults.INCLUDE_PATHS);
        config.setExcludePaths(RecorderDefaults.EXCLUDE_PATHS);
        return config;
    }
}
