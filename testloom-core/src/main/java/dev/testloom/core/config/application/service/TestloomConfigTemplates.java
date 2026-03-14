package dev.testloom.core.config.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides built-in configuration templates used by CLI commands.
 */
public final class TestloomConfigTemplates {
    private static final String TEMPLATE_RESOURCE = "/templates/testloom.yaml";
    private static final String DEFAULT_YAML_TEMPLATE = loadTemplate();

    private TestloomConfigTemplates() {
    }

    /**
     * Returns default content for {@code testloom.yaml}.
     *
     * @return YAML template string
     */
    public static String defaultYaml() {
        return DEFAULT_YAML_TEMPLATE;
    }

    private static String loadTemplate() {
        try (InputStream inputStream = TestloomConfigTemplates.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing config template resource: " + TEMPLATE_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config template resource: " + TEMPLATE_RESOURCE, e);
        }
    }
}
