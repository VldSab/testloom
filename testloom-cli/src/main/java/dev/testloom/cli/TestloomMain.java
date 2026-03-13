package dev.testloom.cli;

import picocli.CommandLine;

public final class TestloomMain {
    private TestloomMain() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TestloomCommand()).execute(args);
        System.exit(exitCode);
    }
}
