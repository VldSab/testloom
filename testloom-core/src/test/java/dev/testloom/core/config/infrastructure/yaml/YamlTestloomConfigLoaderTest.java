package dev.testloom.core.config.infrastructure.yaml;

import dev.testloom.core.config.domain.exception.TestloomConfigException;
import dev.testloom.core.config.domain.exception.TestloomConfigValidationException;
import dev.testloom.core.config.domain.model.RecorderMode;
import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
                    max-body-size-bytes: 65536
                    include-paths: ["/api/**"]
                  redaction:
                    mask: "***"
                    header-default-action: REMOVE
                    query-param-default-action: KEEP
                    json-field-default-action: KEEP
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
    void strictLoadWrapsIoExceptionWhenFileIsNotReadable(@TempDir Path tempDir) throws Exception {
        assumeTrue(Files.getFileStore(tempDir).supportsFileAttributeView("posix"));

        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-bytes: 65536
                    include-paths: ["/api/**"]
                  redaction:
                    mask: "***"
                """);

        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(configPath);
        Files.setPosixFilePermissions(configPath, PosixFilePermissions.fromString("---------"));
        try {
            TestloomConfigException error = assertThrows(TestloomConfigException.class, () -> loader.load(configPath));
            assertThat(error).hasMessageThat().contains("Failed to read YAML config");
        } finally {
            Files.setPosixFilePermissions(configPath, originalPermissions);
        }
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
                    header-default-action: [KEEP
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
                    max-body-size-bytes: 131072
                    include-paths: ["/api/**", " /api/** "]
                  redaction:
                    mask: "###"
                    header-default-action: remove
                    query-param-default-action: keep
                    json-field-default-action: keep
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
        assertThat(config.getRedaction().getHeaderDefaultAction()).isEqualTo(RedactionAction.REMOVE);
        assertThat(config.getRedaction().getQueryParamDefaultAction()).isEqualTo(RedactionAction.KEEP);
        assertThat(config.getRedaction().getJsonFieldDefaultAction()).isEqualTo(RedactionAction.KEEP);
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
                    max-body-size-bytes: 65536
                    include-paths: ["/api/**"]
                    exclude-paths: ["/actuator/**"]
                  redaction:
                    mask: "***"
                    header-default-action: ReMoVe
                    query-param-default-action: KeEp
                    json-field-default-action: kEeP
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
                    max-body-size-bytes: 65536
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
                    max-body-size-bytes: 65536
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
    void configWithUnknownRedactionTargetTypeFailsParsing(@TempDir Path tempDir) throws Exception {
        Path configPath = writeConfig(tempDir, """
                testloom:
                  recorder:
                    enabled: true
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-bytes: 65536
                  redaction:
                    mask: "***"
                    header-default-action: MASK
                    query-param-default-action: MASK
                    json-field-default-action: MASK
                    rules:
                      - type: UNKNOWN_TYPE
                        target: token
                        matcher: EXACT
                        action: MASK
                """);

        TestloomConfigException error = assertThrows(
                TestloomConfigException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("Failed to parse YAML config");
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
                    max-body-size-bytes: 65536
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
                    mode: local
                    output-dir: ./.testloom/captures
                    include-bodies: true
                    max-body-size-bytes: -5
                  redaction:
                    mask: ""
                """);

        TestloomConfigValidationException error = assertThrows(
                TestloomConfigValidationException.class,
                () -> loader.load(configPath)
        );
        assertThat(error).hasMessageThat().contains("max-body-size-bytes");
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
                    max-body-size-bytes: 65536
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
