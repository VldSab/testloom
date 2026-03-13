package dev.testloom.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "testloom",
        description = "CLI for Testloom.",
        mixinStandardHelpOptions = true,
        subcommands = {
                InitCommand.class
        }
)
public class TestloomCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
        spec.commandLine().getOut().flush();
    }
}
