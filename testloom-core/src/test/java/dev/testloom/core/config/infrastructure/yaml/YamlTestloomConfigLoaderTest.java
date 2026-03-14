package dev.testloom.core.config.infrastructure.yaml;

import dev.testloom.core.config.domain.exception.TestloomConfigException;
import dev.testloom.core.config.domain.exception.TestloomConfigValidationException;
import dev.testloom.core.config.domain.model.RecorderMode;
import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests YAML config loading, normalization, and validation rules.
 */
class YamlTestloomConfigLoaderTest {
    private final YamlTestloomConfigLoader loader = new YamlTestloomConfigLoader();

    @Test
    void missingFileReturnsEmptyOptional(@TempDir Path tempDir) {
        Path configPath = tempDir.resolve("testloom.yaml");
        assertThat(loader.loadIfExists(configPath)).isEmpty();
    }

    @Test
    void loadIfExistsRejectsNullPath() {
        assertThrows(IllegalArgumentException.class, () -> loader.loadIfExists(null));
    }

    @Test
    void strictLoadRejectsNullPath() {
        TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(null));
        assertThat(error).hasMessageThat().contains("Config path must not be null.");
    }

    @Test
    void existingFileLoadsViaOptionalApi(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-kb: 64
                    include-paths: ["/api/**"]
                  redaction:
                    mask: "***"
                    headers: ["authorization"]
                    json-fields: ["password"]
                    query-params: ["token"]
                """);

        TestloomConfig config = loader.loadIfExists(configPath).orElseThrow();

        assertThat(config).isNotNull();
        assertThat(config.getRecorder().getOutputDir()).isEqualTo("./.testloom/captures");
        assertThat(config.getRedaction().getMask()).isEqualTo("***");
    }

    @Test
    void strictLoadFailsForMissingFile(@TempDir Path tempDir) {
        Path configPath = tempDir.resolve("testloom.yaml");
        assertThrows(TestloomConfigException.class, () -> loader.load(configPath));
    }

    @Test
    void strictLoadFailsWhenTopLevelTestloomKeyIsMissing(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, "{}");
        TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(configPath));
        assertThat(error).hasMessageThat().contains("top-level 'testloom' key");
    }

    @Test
    void strictLoadFailsOnUnknownProperties(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    unknown-field: value
                  redaction:
                    mask: "***"
                """);
        TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(configPath));
        assertThat(error).hasMessageThat().contains("Failed to parse YAML config");
    }

    @Test
    void strictLoadFailsOnInvalidYamlSyntax(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                  redaction:
                    mask: "***"
                    headers: [authorization
                """);
        TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(configPath));
        assertThat(error).hasMessageThat().contains("Failed to parse YAML config");
    }

    @Test
    void strictLoadFailsWhenPathPointsToDirectory(@TempDir Path tempDir) throws Exception {
        Path directory = tempDir.resolve("dir-as-file");
        Files.createDirectories(directory);

        TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(directory));
        assertThat(error).hasMessageThat().contains("YAML config");
    }

    @Test
    void parsesAndNormalizesConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: staging
                    output-dir: ./captures
                    include-bodies: true
                    max-body-size-kb: 128
                    include-paths: ["/api/**", " /api/** "]
                  redaction:
                    mask: "###"
                    headers: [" Authorization ", "authorization", "X-API-Key"]
                    json-fields: ["password", " password "]
                    query-params: ["token", "Token"]
                    rules:
                      - type: HEADER
                        target: " Authorization "
                        matcher: EXACT
                        action: MASK
                      - type: QUERY_PARAM
                        target: " token "
                        matcher: EXACT
                        action: REMOVE
                """);

        TestloomConfig config = loader.load(configPath);

        assertThat(config.getRecorder().getMode()).isEqualTo(RecorderMode.STAGING);
        assertThat(config.getRecorder().getOutputDir()).isEqualTo("./captures");
        assertThat(config.getRecorder().getIncludePaths()).containsExactly("/api/**").inOrder();
        assertThat(config.getRedaction().getHeaders()).containsExactly("authorization", "x-api-key").inOrder();
        assertThat(config.getRedaction().getRules()).hasSize(2);
        assertThat(config.getRedaction().getRules().get(0).getTarget()).isEqualTo("authorization");
        assertThat(config.getRedaction().getRules().get(1).getTarget()).isEqualTo("token");
    }

    @Test
    void parsesEnumsCaseInsensitively(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: StAgInG
                    output-dir: ./captures
                    include-bodies: true
                    max-body-size-kb: 64
                    include-paths: ["/api/**"]
                    exclude-paths: ["/actuator/**"]
                  redaction:
                    mask: "***"
                    headers: ["authorization"]
                    json-fields: ["password"]
                    query-params: ["token"]
                    rules:
                      - type: HeAdEr
                        target: Authorization
                        matcher: ExAcT
                        action: MaSk
                """);

        TestloomConfig config = loader.load(configPath);

        assertThat(config.getRecorder().getMode()).isEqualTo(RecorderMode.STAGING);
        assertThat(config.getRedaction().getRules().get(0).getTarget()).isEqualTo("authorization");
    }

    @Test
    void configWithMissingSectionsFailsLint(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, "testloom: {}");

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("testloom.recorder must not be null.");
        assertThat(error).hasMessageThat().contains("testloom.redaction must not be null.");
    }

    @Test
    void configWithNullRuleFailsLint(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-kb: 64
                  redaction:
                    mask: "***"
                    rules:
                      - null
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("testloom.redaction.rules[0] must not be null.");
    }

    @Test
    void configWithInvalidRegexRuleFailsLint(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-kb: 64
                  redaction:
                    mask: "***"
                    rules:
                      - type: HEADER
                        target: "["
                        matcher: REGEX
                        action: MASK
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("not a valid regex");
    }

    @Test
    void configWithIncompleteRuleFailsLint(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-kb: 64
                  redaction:
                    mask: "***"
                    rules:
                      - target: "  "
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains(".type must not be null.");
        assertThat(error).hasMessageThat().contains(".matcher must not be null.");
        assertThat(error).hasMessageThat().contains(".action must not be null.");
        assertThat(error).hasMessageThat().contains(".target must not be blank.");
    }

    @Test
    void invalidConfigFailsValidation(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    max-body-size-kb: -5
                  redaction:
                    mask: ""
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("max-body-size-kb");
        assertThat(error).hasMessageThat().contains("redaction.mask");
    }

    @Test
    void configWithNullOutputDirFailsValidation(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    include-bodies: true
                    max-body-size-kb: 64
                  redaction:
                    mask: "***"
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("output-dir must not be blank");
    }

    private static Path writeConfig(Path tempDir, String yaml) throws Exception {
        Path configPath = tempDir.resolve("testloom.yaml");
        Files.writeString(configPath, yaml, StandardCharsets.UTF_8);
        return configPath;
    }
}
