package dev.testloom.spring.mvc;

import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests path matching behavior for MVC capture include/exclude patterns.
 */
class AntPatternMvcCapturePathMatcherTest {
    private final AntPatternMvcCapturePathMatcher matcher = new AntPatternMvcCapturePathMatcher();

    @Test
    void shouldCaptureUsesPathWithoutContextPath() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(List.of("/api/**"));
        config.getRecorder().setExcludePaths(List.of("/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/api/hello");
        request.setContextPath("/app");

        assertThat(matcher.shouldCapture(request, config)).isTrue();
    }

    @Test
    void shouldCaptureRespectsExcludePatterns() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(List.of("/api/**"));
        config.getRecorder().setExcludePaths(List.of("/api/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/health");

        assertThat(matcher.shouldCapture(request, config)).isFalse();
    }

    @Test
    void shouldNotCaptureWhenIncludePatternsAreMissing() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(List.of());
        config.getRecorder().setExcludePaths(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");

        assertThat(matcher.shouldCapture(request, config)).isFalse();
    }

    @Test
    void shouldCaptureWhenIncludedAndNotExcluded() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(List.of("/api/**"));
        config.getRecorder().setExcludePaths(List.of("/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");

        assertThat(matcher.shouldCapture(request, config)).isTrue();
    }

    @Test
    void shouldIgnoreNullPatternEntries() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(Arrays.asList(null, "/api/**"));
        config.getRecorder().setExcludePaths(Arrays.asList((String) null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");

        assertThat(matcher.shouldCapture(request, config)).isTrue();
    }

    @Test
    void shouldNotCaptureWhenRequestPathIsBlank() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setIncludePaths(List.of("/api/**"));
        config.getRecorder().setExcludePaths(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");

        assertThat(matcher.shouldCapture(request, config)).isFalse();
    }

    @Test
    void shouldFailFastWhenConfigIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");

        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> matcher.shouldCapture(request, null)
        );

        assertThat(error).hasMessageThat().contains("config must not be null");
    }

    @Test
    void shouldFailFastWhenRecorderSectionIsNull() {
        TestloomConfig config = new TestloomConfig();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");

        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> matcher.shouldCapture(request, config)
        );

        assertThat(error).hasMessageThat().contains("testloom.recorder must not be null");
    }
}
