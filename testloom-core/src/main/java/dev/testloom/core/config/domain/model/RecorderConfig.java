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
    @JsonProperty("max-body-size-kb")
    private int maxBodySizeKb;
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
        config.setEnabled(false);
        config.setMode(RecorderMode.LOCAL);
        config.setOutputDir("./.testloom/captures");
        config.setIncludeBodies(true);
        config.setMaxBodySizeKb(64);
        config.setIncludePaths(List.of("/api/**"));
        config.setExcludePaths(List.of("/actuator/**"));
        return config;
    }
}
