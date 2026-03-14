package dev.testloom.cli;

import dev.testloom.core.config.application.service.TestloomConfigTemplates;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Creates a default {@code testloom.yaml} configuration file.
 */
@Command(
        name = "init",
        description = "Initialize Testloom project scaffold.",
        mixinStandardHelpOptions = true
)
public class InitCommand implements Runnable {
    private static final String CONFIG_FILE_NAME = "testloom.yaml";
    private static final String ALTERNATE_CONFIG_FILE_NAME = "testloom.yml";

    @Option(
            names = "--path",
            description = "Path where testloom config will be written."
    )
    Path path;

    @Option(
            names = "--force",
            description = "Overwrite existing config file if present."
    )
    boolean force;

    @Spec
    CommandSpec spec;

    /**
     * Writes config template to the target location.
     */
    @Override
    public void run() {
        Path outputPath = resolveOutputPath();
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (force) {
                Files.writeString(
                        outputPath,
                        TestloomConfigTemplates.defaultYaml(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            } else {
                Files.writeString(
                        outputPath,
                        TestloomConfigTemplates.defaultYaml(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW
                );
            }
        } catch (FileAlreadyExistsException e) {
            throw new ParameterException(spec.commandLine(), "Config already exists at " + outputPath + ". Use --force to overwrite.");
        } catch (IOException e) {
            throw new ParameterException(spec.commandLine(), "Failed to write config at " + outputPath + ": " + e.getMessage(), e);
        }

        spec.commandLine().getOut().println("Created config: " + outputPath);
    }

    private Path resolveOutputPath() {
        if (path != null) {
            Path resolved = path.toAbsolutePath().normalize();
            validateConfigPath(resolved);
            return resolved;
        }

        Path projectRoot = findProjectRoot(Path.of("").toAbsolutePath().normalize());
        return projectRoot.resolve(CONFIG_FILE_NAME);
    }

    private void validateConfigPath(Path configPath) {
        String fileName = configPath.getFileName() == null ? "" : configPath.getFileName().toString();
        if (fileName.isBlank()) {
            throw new ParameterException(spec.commandLine(), "Config path must include a file name.");
        }

        if (!(fileName.endsWith(".yaml") || fileName.endsWith(".yml"))) {
            throw new ParameterException(spec.commandLine(), "Config file extension must be .yaml or .yml.");
        }

        if (!(CONFIG_FILE_NAME.equals(fileName) || ALTERNATE_CONFIG_FILE_NAME.equals(fileName))) {
            throw new ParameterException(
                    spec.commandLine(),
                    "Config file name must be 'testloom.yaml' or 'testloom.yml'."
            );
        }
    }

    private static Path findProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts")) || Files.isDirectory(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return start;
    }
}
