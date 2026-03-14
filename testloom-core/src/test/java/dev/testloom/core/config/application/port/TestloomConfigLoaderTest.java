package dev.testloom.core.config.application.port;

import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests default API behavior of {@link TestloomConfigLoader}.
 */
class TestloomConfigLoaderTest {
    @Test
    void loadIfExistsRejectsNullPath() {
        TestloomConfigLoader loader = path -> TestloomConfig.defaults();
        assertThrows(IllegalArgumentException.class, () -> loader.loadIfExists(null));
    }

    @Test
    void loadIfExistsReturnsEmptyForMissingFile(@TempDir Path tempDir) {
        TestloomConfigLoader loader = path -> TestloomConfig.defaults();
        Path missing = tempDir.resolve("missing.yaml");

        assertThat(loader.loadIfExists(missing)).isEmpty();
    }

    @Test
    void loadIfExistsDelegatesToLoadForExistingFile(@TempDir Path tempDir) throws Exception {
        Path existing = tempDir.resolve("testloom.yaml");
        Files.writeString(existing, "testloom: {}", StandardCharsets.UTF_8);

        TestloomConfig expected = TestloomConfig.defaults();
        StubLoader loader = new StubLoader(expected);

        TestloomConfig result = loader.loadIfExists(existing).orElseThrow();

        assertThat(loader.calls).isEqualTo(1);
        assertThat(loader.lastPath).isSameInstanceAs(existing);
        assertThat(result).isSameInstanceAs(expected);
    }

    private static final class StubLoader implements TestloomConfigLoader {
        private final TestloomConfig result;
        private int calls;
        private Path lastPath;

        private StubLoader(TestloomConfig result) {
            this.result = result;
        }

        @Override
        public TestloomConfig load(Path path) {
            calls++;
            lastPath = path;
            return result;
        }
    }
}
