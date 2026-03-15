package dev.testloom.core.config.domain.model;

import java.util.List;

/**
 * Shared default values for recorder configuration.
 */
public final class RecorderDefaults {
    public static final boolean ENABLED = false;
    public static final RecorderMode MODE = RecorderMode.LOCAL;
    public static final String OUTPUT_DIR = "./.testloom/captures";
    public static final boolean INCLUDE_BODIES = true;
    public static final int MAX_BODY_SIZE_BYTES = 64 * 1024;
    public static final List<String> INCLUDE_PATHS = List.of("/api/**");
    public static final List<String> EXCLUDE_PATHS = List.of("/actuator/**");

    private RecorderDefaults() {
    }
}
