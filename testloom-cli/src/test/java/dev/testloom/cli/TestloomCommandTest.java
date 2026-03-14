package dev.testloom.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests core behavior of root CLI command and init command.
 */
class TestloomCommandTest {
    @Test
    void noArgsShowsRootCommandUsage() {
        CliHarness cli = CliHarness.create();

        int exitCode = cli.execute();

        assertThat(exitCode).isEqualTo(0);
        assertThat(cli.stdout()).contains("Usage: testloom");
    }

    @Test
    void helpShowsRootCommandUsage() {
        CliHarness cli = CliHarness.create();

        int exitCode = cli.execute("--help");

        assertThat(exitCode).isEqualTo(0);
        assertThat(cli.stdout()).contains("Usage: testloom");
        assertThat(cli.stdout()).contains("init");
    }

    @Test
    void versionShowsVersionString() {
        CliHarness cli = CliHarness.create();

        int exitCode = cli.execute("--version");

        assertThat(exitCode).isEqualTo(0);
        assertThat(cli.stdout()).contains("testloom 0.1.0");
    }

    @Test
    void unknownCommandReturnsUsageError() {
        CliHarness cli = CliHarness.create();

        int exitCode = cli.execute("unknown");

        assertThat(exitCode).isEqualTo(2);
        assertThat(cli.stderr()).contains("Unmatched argument");
    }

    @Test
    void initHelpShowsInitUsage() {
        CliHarness cli = CliHarness.create();

        int exitCode = cli.execute("init", "--help");

        assertThat(exitCode).isEqualTo(0);
        assertThat(cli.stdout()).contains("Usage: testloom init");
        assertThat(cli.stdout()).contains("--force");
    }

    @Test
    void initCommandCreatesConfigFile(@TempDir Path tempDir) throws Exception {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("testloom.yaml");

        int exitCode = cli.execute("init", "--path", configPath.toString());

        assertThat(exitCode).isEqualTo(0);
        assertThat(cli.stdout()).contains("Created config");
        assertThat(Files.exists(configPath)).isTrue();
        assertThat(Files.readString(configPath)).contains("testloom:");
    }

    @Test
    void initCommandCreatesMissingParentDirectories(@TempDir Path tempDir) {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("a/b/c/testloom.yaml");

        int exitCode = cli.execute("init", "--path", configPath.toString());

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(configPath)).isTrue();
    }

    @Test
    void initCommandFailsWithoutForceWhenFileExists(@TempDir Path tempDir) throws Exception {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("testloom.yaml");
        Files.writeString(configPath, "existing", StandardCharsets.UTF_8);

        int exitCode = cli.execute("init", "--path", configPath.toString());

        assertThat(exitCode).isEqualTo(2);
        assertThat(cli.stderr()).contains("Config already exists");
        assertThat(Files.readString(configPath)).isEqualTo("existing");
    }

    @Test
    void initCommandFailsWhenPathPointsToDirectory(@TempDir Path tempDir) throws Exception {
        CliHarness cli = CliHarness.create();
        Path directoryPath = tempDir.resolve("testloom.yaml");
        Files.createDirectories(directoryPath);

        int exitCode = cli.execute("init", "--path", directoryPath.toString());

        assertThat(exitCode).isEqualTo(2);
        assertThat(cli.stderr().contains("Failed to write config") || cli.stderr().contains("Config already exists")).isTrue();
    }

    @Test
    void initCommandOverwritesWhenForceIsEnabled(@TempDir Path tempDir) throws Exception {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("testloom.yaml");
        Files.writeString(configPath, "existing", StandardCharsets.UTF_8);

        int exitCode = cli.execute("init", "--path", configPath.toString(), "--force");

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.readString(configPath)).contains("testloom:");
    }

    @Test
    void initCommandRejectsInvalidFileExtension(@TempDir Path tempDir) {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("testloom.txt");

        int exitCode = cli.execute("init", "--path", configPath.toString());

        assertThat(exitCode).isEqualTo(2);
        assertThat(cli.stderr()).contains("Config file extension must be .yaml or .yml.");
    }

    @Test
    void initCommandRejectsUnexpectedFileName(@TempDir Path tempDir) {
        CliHarness cli = CliHarness.create();
        Path configPath = tempDir.resolve("config.yaml");

        int exitCode = cli.execute("init", "--path", configPath.toString());

        assertThat(exitCode).isEqualTo(2);
        assertThat(cli.stderr()).contains("Config file name must be 'testloom.yaml' or 'testloom.yml'.");
    }

    @Test
    void initCommandFailsWithPermissionDeniedWritePath(@TempDir Path tempDir) throws Exception {
        assumeTrue(Files.getFileStore(tempDir).supportsFileAttributeView("posix"));

        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyDir);
        Path configPath = readOnlyDir.resolve("testloom.yaml");

        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(readOnlyDir);
        Files.setPosixFilePermissions(readOnlyDir, PosixFilePermissions.fromString("r-xr-xr-x"));

        try {
            CliHarness cli = CliHarness.create();
            int exitCode = cli.execute("init", "--path", configPath.toString());

            assertThat(exitCode).isEqualTo(2);
            assertThat(cli.stderr()).contains("Failed to write config");
        } finally {
            Files.setPosixFilePermissions(readOnlyDir, originalPermissions);
        }
    }

    @Test
    void findProjectRootReturnsNearestAncestorWithSettingsGradle(@TempDir Path tempDir) throws Exception {
        Path projectRoot = tempDir.resolve("repo");
        Path moduleDir = projectRoot.resolve("testloom-cli");
        Files.createDirectories(moduleDir);
        Files.writeString(projectRoot.resolve("settings.gradle.kts"), "rootProject.name = \"testloom\"", StandardCharsets.UTF_8);

        Path detected = invokeFindProjectRoot(moduleDir);

        assertThat(detected).isEqualTo(projectRoot);
    }

    @Test
    void findProjectRootFallsBackToProvidedStartWhenMarkersMissing(@TempDir Path tempDir) throws Exception {
        Path start = tempDir.resolve("plain-dir");
        Files.createDirectories(start);

        Path detected = invokeFindProjectRoot(start);

        assertThat(detected).isEqualTo(start);
    }

    private static Path invokeFindProjectRoot(Path start) throws Exception {
        Method method = InitCommand.class.getDeclaredMethod("findProjectRoot", Path.class);
        method.setAccessible(true);
        return (Path) method.invoke(null, start);
    }

    private static final class CliHarness {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final CommandLine commandLine = new CommandLine(new TestloomCommand());

        private CliHarness() {
            commandLine.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));
            commandLine.setErr(new PrintWriter(err, true, StandardCharsets.UTF_8));
        }

        static CliHarness create() {
            return new CliHarness();
        }

        int execute(String... args) {
            return commandLine.execute(args);
        }

        String stdout() {
            return out.toString(StandardCharsets.UTF_8);
        }

        String stderr() {
            return err.toString(StandardCharsets.UTF_8);
        }
    }
}
