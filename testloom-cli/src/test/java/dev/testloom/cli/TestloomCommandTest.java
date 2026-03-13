package dev.testloom.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestloomCommandTest {
    @Test
    void noArgsShowsRootCommandUsage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new TestloomCommand());
        commandLine.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));

        int exitCode = commandLine.execute();
        String output = out.toString(StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(output.contains("Usage: testloom"), "Expected usage output for testloom");
    }

    @Test
    void helpShowsRootCommandUsage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new TestloomCommand());
        commandLine.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));

        int exitCode = commandLine.execute("--help");
        String output = out.toString(StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(output.contains("Usage: testloom"), "Expected usage output for testloom");
        assertTrue(output.contains("init"), "Expected init subcommand to be listed");
    }

    @Test
    void initCommandExecutesStub() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new TestloomCommand());
        commandLine.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));

        int exitCode = commandLine.execute("init");
        String output = out.toString(StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(output.contains("stub command"), "Expected stub init output");
    }
}
