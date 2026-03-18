package dev.testloom.core.config.application.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertThat(yaml).contains("header-default-action:");
        assertThat(yaml).contains("query-param-default-action:");
        assertThat(yaml).contains("json-field-default-action:");
        assertThat(yaml).contains("rules:");
    }

    @Test
    void defaultYamlIsCachedAndStable() {
        String first = TestloomConfigTemplates.defaultYaml();
        String second = TestloomConfigTemplates.defaultYaml();

        assertThat(first).isSameInstanceAs(second);
    }

    @Test
    void loadTemplateThrowsWhenResourceIsMissing() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> TestloomConfigTemplates.loadTemplate(() -> null)
        );

        assertThat(error).hasMessageThat().contains("Missing config template resource");
    }

    @Test
    void loadTemplateWrapsIOException() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> TestloomConfigTemplates.loadTemplate(() -> new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom");
                    }
                })
        );

        assertThat(error).hasMessageThat().contains("Failed to read config template resource");
        assertThat(error.getCause()).isInstanceOf(IOException.class);
    }
}
