package dev.testloom.spring.mvc;

import dev.testloom.spring.properties.TestloomProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests path matching behavior for MVC capture include/exclude patterns.
 */
class AntPatternMvcCapturePathMatcherTest {
    private final AntPatternMvcCapturePathMatcher matcher = new AntPatternMvcCapturePathMatcher();

    @Test
    void shouldCaptureUsesPathWithoutContextPath() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(List.of("/api/**"));
        recorder.setExcludePaths(List.of("/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/api/hello");
        request.setContextPath("/app");

        assertThat(matcher.shouldCapture(request, recorder)).isTrue();
    }

    @Test
    void shouldCaptureRespectsExcludePatterns() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(List.of("/api/**"));
        recorder.setExcludePaths(List.of("/api/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/health");

        assertThat(matcher.shouldCapture(request, recorder)).isFalse();
    }

    @Test
    void shouldNotCaptureWhenIncludePatternsAreMissing() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(List.of());
        recorder.setExcludePaths(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");

        assertThat(matcher.shouldCapture(request, recorder)).isFalse();
    }

    @Test
    void shouldCaptureWhenIncludedAndNotExcluded() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(List.of("/api/**"));
        recorder.setExcludePaths(List.of("/internal/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");

        assertThat(matcher.shouldCapture(request, recorder)).isTrue();
    }

    @Test
    void shouldIgnoreNullPatternEntries() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(Arrays.asList(null, "/api/**"));
        recorder.setExcludePaths(Arrays.asList((String) null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");

        assertThat(matcher.shouldCapture(request, recorder)).isTrue();
    }

    @Test
    void shouldNotCaptureWhenRequestPathIsBlank() {
        TestloomProperties.RecorderProperties recorder = new TestloomProperties.RecorderProperties();
        recorder.setIncludePaths(List.of("/api/**"));
        recorder.setExcludePaths(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");

        assertThat(matcher.shouldCapture(request, recorder)).isFalse();
    }
}
