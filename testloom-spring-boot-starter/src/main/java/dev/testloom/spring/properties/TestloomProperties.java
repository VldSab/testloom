package dev.testloom.spring.properties;

import dev.testloom.core.config.domain.model.RecorderDefaults;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code testloom.*} runtime properties for the Spring starter module.
 */
@Getter
@ConfigurationProperties(prefix = "testloom")
public class TestloomProperties {
    /**
     * Recorder configuration used by the HTTP capture filter.
     */
    private RecorderProperties recorder = new RecorderProperties();

    /**
     * Sets recorder settings.
     *
     * <p>If the incoming value is {@code null}, defaults are restored to keep
     * the properties object in a valid state.
     *
     * @param recorder recorder settings loaded from configuration
     */
    public void setRecorder(RecorderProperties recorder) {
        this.recorder = recorder == null ? new RecorderProperties() : recorder;
    }

    /**
     * Binds {@code testloom.recorder.*} settings.
     */
    @Getter
    @Setter
    public static class RecorderProperties {
        /**
         * Enables request/response recording when set to {@code true}.
         */
        private boolean enabled = RecorderDefaults.ENABLED;
        /**
         * Target directory for capture JSON files.
         */
        private String outputDir = RecorderDefaults.OUTPUT_DIR;
        /**
         * Enables storing request/response bodies.
         */
        private boolean includeBodies = RecorderDefaults.INCLUDE_BODIES;
        /**
         * Maximum body size (in bytes) written to capture files.
         */
        private int maxBodySizeBytes = RecorderDefaults.MAX_BODY_SIZE_BYTES;
        /**
         * Include patterns for request paths (Ant-style patterns).
         */
        private List<String> includePaths = RecorderDefaults.INCLUDE_PATHS;
        /**
         * Exclude patterns for request paths (Ant-style patterns).
         */
        private List<String> excludePaths = RecorderDefaults.EXCLUDE_PATHS;
    }
}
