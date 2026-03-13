package dev.testloom.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "init",
        description = "Initialize Testloom project scaffold (stub)."
)
public class InitCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().getOut().println("testloom init: stub command (implementation pending).");
    }
}
