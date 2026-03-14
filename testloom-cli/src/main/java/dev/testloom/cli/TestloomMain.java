package dev.testloom.cli;

import picocli.CommandLine;

/**
 * Entry point for the Testloom CLI process.
 */
public final class TestloomMain {
    private TestloomMain() {
    }

    /**
     * Runs the root Picocli command and exits with its status code.
     *
     * @param args CLI arguments passed by the user
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TestloomCommand()).execute(args);
        System.exit(exitCode);
    }
}
