package dev.testloom.spring.mvc;

import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests servlet filter behavior for MVC capture.
 */
class MvcCaptureFilterTest {
    @Test
    void capturesEnvelopeWithCoreRequestAndResponseData() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setQueryString("expand=true");
        request.setContentType("application/json");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.addHeader("x-req", "abc");
        request.setContent("{\"name\":\"demo\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            StreamUtils.copyToByteArray(servletRequest.getInputStream());
            servletResponse.setContentType("application/json");
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.addHeader("x-res", "ok");
            httpResponse.setStatus(201);
            servletResponse.getWriter().write("{\"id\":1}");
        };

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(1);
        CaptureEnvelope envelope = recorder.lastEnvelope.get();
        assertThat(envelope).isNotNull();
        assertThat(envelope.schemaVersion()).isEqualTo("0.1.0");
        assertThat(envelope.transport()).isEqualTo("HTTP");
        assertThat(envelope.recordedAt()).isNotEmpty();
        assertThat(envelope.request().method()).isEqualTo("POST");
        assertThat(envelope.request().path()).isEqualTo("/api/orders");
        assertThat(envelope.request().query()).isEqualTo("expand=true");
        assertThat(envelope.request().headers()).containsKey("x-req");
        assertThat(envelope.request().body()).contains("\"name\":\"demo\"");
        assertThat(envelope.request().truncation()).isNotNull();
        assertThat(envelope.request().truncation().bodyTruncated()).isFalse();
        assertThat(envelope.request().truncation().capturedSizeBytes())
                .isEqualTo(envelope.request().truncation().originalSizeBytes());
        assertThat(envelope.response().status()).isEqualTo(201);
        assertThat(envelope.response().headers()).containsKey("x-res");
        assertThat(envelope.response().body()).contains("\"id\":1");
        assertThat(envelope.response().truncation()).isNotNull();
        assertThat(envelope.response().truncation().bodyTruncated()).isFalse();
        assertThat(envelope.response().durationMs()).isAtLeast(0L);
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":1}");
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void includeBodiesFalseStoresNullBodies() throws Exception {
        TestloomConfig config = defaultConfig();
        config.getRecorder().setIncludeBodies(false);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContentType("application/json");
        request.setContent("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            try {
                StreamUtils.copyToByteArray(servletRequest.getInputStream());
                servletResponse.getWriter().write("{\"ok\":true}");
            } catch (IOException e) {
                throw new ServletException(e);
            }
        };

        filter.doFilter(request, response, chain);

        CaptureEnvelope envelope = recorder.lastEnvelope.get();
        assertThat(envelope.request().body()).isNull();
        assertThat(envelope.response().body()).isNull();
        assertThat(envelope.request().truncation()).isNotNull();
        assertThat(envelope.request().truncation().bodyTruncated()).isFalse();
        assertThat(envelope.request().truncation().capturedSizeBytes()).isEqualTo(0);
        assertThat(envelope.request().truncation().originalSizeBytes()).isGreaterThan(0);
        assertThat(envelope.response().truncation()).isNotNull();
        assertThat(envelope.response().truncation().bodyTruncated()).isFalse();
        assertThat(envelope.response().truncation().capturedSizeBytes()).isEqualTo(0);
        assertThat(envelope.response().truncation().originalSizeBytes()).isAtLeast(0);
    }

    @Test
    void bodyIsTruncatedByConfiguredMaxBodySize() throws Exception {
        TestloomConfig config = defaultConfig();
        config.getRecorder().setMaxBodySizeBytes(4);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContent("123456".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            StreamUtils.copyToByteArray(servletRequest.getInputStream());
            servletResponse.getWriter().write("abcdef");
        };

        filter.doFilter(request, response, chain);

        CaptureEnvelope envelope = recorder.lastEnvelope.get();
        assertThat(envelope.request().body()).isEqualTo("1234");
        assertThat(envelope.response().body()).isEqualTo("abcd");
        assertThat(envelope.request().truncation())
                .isEqualTo(new CaptureEnvelope.Truncation(true, 6, 4));
        assertThat(envelope.response().truncation())
                .isEqualTo(new CaptureEnvelope.Truncation(true, 6, 4));
    }

    @Test
    void includeBodiesFalseAllowsNonPositiveMaxBodySize() throws Exception {
        TestloomConfig config = defaultConfig();
        config.getRecorder().setIncludeBodies(false);
        config.getRecorder().setMaxBodySizeBytes(0);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("ok");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(1);
    }

    @Test
    void invalidRequestEncodingFallsBackToUtf8() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        String body = "привет";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setCharacterEncoding("NOT_A_CHARSET");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            StreamUtils.copyToByteArray(servletRequest.getInputStream());
            servletResponse.getWriter().write("ok");
        };

        filter.doFilter(request, response, chain);

        assertThat(recorder.lastEnvelope.get().request().body()).isEqualTo(body);
    }

    @Test
    void invalidResponseEncodingFallsBackToUtf8() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        String responseBody = "привет";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            servletResponse.setCharacterEncoding("NOT_A_CHARSET");
            servletResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        assertThat(recorder.lastEnvelope.get().response().body()).isEqualTo(responseBody);
    }

    @Test
    void captureRecorderFailureDoesNotBreakHttpResponse() throws Exception {
        TestloomConfig config = defaultConfig();
        CaptureRecorder failingRecorder = envelope -> {
            throw new RuntimeException("capture failed");
        };

        MvcCaptureFilter filter = new MvcCaptureFilter(failingRecorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("hello");

        filter.doFilter(request, response, chain);

        assertThat(response.getContentAsString()).isEqualTo("hello");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void requestOutsideIncludePatternsIsNotRecorded() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("up");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("up");
    }

    @Test
    void excludedRequestIsNotRecorded() throws Exception {
        TestloomConfig config = defaultConfig();
        config.getRecorder().setIncludePaths(List.of("/**"));
        config.getRecorder().setExcludePaths(List.of("/api/internal/**"));
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("blocked");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("blocked");
    }

    @Test
    void blankPathIsNotRecorded() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("blank");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("blank");
    }

    @Test
    void disabledRecorderSkipsCapture() throws Exception {
        TestloomConfig config = defaultConfig();
        config.getRecorder().setEnabled(false);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("ok");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    @Test
    void supportsAlreadyWrappedRequestAndResponse() throws Exception {
        TestloomConfig config = defaultConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/hello");
        rawRequest.setContent("hello".getBytes(StandardCharsets.UTF_8));
        var wrappedRequest = new org.springframework.web.util.ContentCachingRequestWrapper(rawRequest, 1024);

        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        var wrappedResponse = new org.springframework.web.util.ContentCachingResponseWrapper(rawResponse);

        FilterChain chain = (servletRequest, servletResponse) -> {
            StreamUtils.copyToByteArray(((org.springframework.web.util.ContentCachingRequestWrapper) servletRequest).getInputStream());
            servletResponse.getWriter().write("pong");
        };

        filter.doFilter(wrappedRequest, wrappedResponse, chain);

        assertThat(recorder.calls.get()).isEqualTo(1);
        assertThat(rawResponse.getContentAsString()).isEqualTo("pong");
    }

    @Test
    void missingRecorderConfigFailsFast() {
        TestloomConfig config = new TestloomConfig();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, config, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> { };

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> filter.doFilter(request, response, chain)
        );

        assertThat(error).hasMessageThat().contains("testloom.recorder must not be null");
    }

    private static TestloomConfig defaultConfig() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setEnabled(true);
        config.getRecorder().setIncludePaths(List.of("/api/**"));
        config.getRecorder().setExcludePaths(List.of());
        config.getRecorder().setIncludeBodies(true);
        config.getRecorder().setMaxBodySizeBytes(1024);
        return config;
    }

    private static final class CapturingRecorder implements CaptureRecorder {
        private final AtomicReference<CaptureEnvelope> lastEnvelope = new AtomicReference<>();
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public void record(CaptureEnvelope envelope) {
            lastEnvelope.set(envelope);
            calls.incrementAndGet();
        }
    }
}
