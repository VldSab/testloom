package dev.testloom.core.config.application.service;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests built-in config template provider.
 */
class TestloomConfigTemplatesTest {
    @Test
    void defaultYamlContainsExpectedRootAndSections() {
        String yaml = TestloomConfigTemplates.defaultYaml();

        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("testloom:");
        assertThat(yaml).contains("recorder:");
        assertThat(yaml).contains("redaction:");
        assertThat(yaml).contains("rules:");
    }

    @Test
    void defaultYamlIsCachedAndStable() {
        String first = TestloomConfigTemplates.defaultYaml();
        String second = TestloomConfigTemplates.defaultYaml();

        assertThat(first).isSameInstanceAs(second);
    }
}
