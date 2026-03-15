package dev.testloom.spring.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests path normalization utility for servlet requests.
 */
class ServletRequestPathsTest {
    @Test
    void returnsEmptyWhenUriIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");

        assertThat(ServletRequestPaths.applicationPath(request)).isEmpty();
    }

    @Test
    void returnsUriWhenContextPathIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");

        assertThat(ServletRequestPaths.applicationPath(request)).isEqualTo("/api/hello");
    }

    @Test
    void stripsContextPathPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/api/hello");
        request.setContextPath("/app");

        assertThat(ServletRequestPaths.applicationPath(request)).isEqualTo("/api/hello");
    }

    @Test
    void returnsRootWhenUriEqualsContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app");
        request.setContextPath("/app");

        assertThat(ServletRequestPaths.applicationPath(request)).isEqualTo("/");
    }

    @Test
    void keepsUriWhenContextPathIsNotPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        request.setContextPath("/app");

        assertThat(ServletRequestPaths.applicationPath(request)).isEqualTo("/api/hello");
    }
}
