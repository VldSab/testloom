package dev.testloom.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Root command for the {@code testloom} CLI.
 */
@Command(
        name = "testloom",
        description = "CLI for Testloom.",
        mixinStandardHelpOptions = true,
        version = "testloom 0.1.0",
        subcommands = {
                InitCommand.class
        }
)
public class TestloomCommand implements Runnable {
    @Spec
    CommandSpec spec;

    /**
     * Shows command usage when no subcommand is provided.
     */
    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
        spec.commandLine().getOut().flush();
    }
}
