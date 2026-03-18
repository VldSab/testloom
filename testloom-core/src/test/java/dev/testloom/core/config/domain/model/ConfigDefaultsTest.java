package dev.testloom.core.config.domain.model;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests default config values.
 */
class ConfigDefaultsTest {
    @Test
    void recorderDefaultsAreStable() {
        RecorderConfig defaults = RecorderConfig.defaults();

        assertThat(defaults.isEnabled()).isFalse();
        assertThat(defaults.getMode()).isEqualTo(RecorderMode.LOCAL);
        assertThat(defaults.getOutputDir()).isEqualTo("./.testloom/captures");
        assertThat(defaults.isIncludeBodies()).isTrue();
        assertThat(defaults.getMaxBodySizeBytes()).isEqualTo(65536);
        assertThat(defaults.getIncludePaths()).containsExactly("/api/**").inOrder();
        assertThat(defaults.getExcludePaths()).containsExactly("/actuator/**").inOrder();
    }

    @Test
    void redactionDefaultsAreStable() {
        RedactionConfig defaults = RedactionConfig.defaults();

        assertThat(defaults.getMask()).isEqualTo("***");
        assertThat(defaults.getHeaderDefaultAction()).isEqualTo(RedactionAction.MASK);
        assertThat(defaults.getQueryParamDefaultAction()).isEqualTo(RedactionAction.MASK);
        assertThat(defaults.getJsonFieldDefaultAction()).isEqualTo(RedactionAction.MASK);
        assertThat(defaults.getRules()).isEmpty();
    }

    @Test
    void testloomDefaultsComposeNestedDefaults() {
        TestloomConfig defaults = TestloomConfig.defaults();

        assertThat(defaults.getRecorder()).isNotNull();
        assertThat(defaults.getRedaction()).isNotNull();
        assertThat(defaults.getRecorder().getMode()).isEqualTo(RecorderMode.LOCAL);
        assertThat(defaults.getRedaction().getMask()).isEqualTo("***");
    }
}
