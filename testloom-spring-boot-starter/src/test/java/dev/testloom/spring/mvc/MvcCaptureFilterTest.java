package dev.testloom.spring.mvc;

import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.spring.properties.TestloomProperties;
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

/**
 * Tests servlet filter behavior for MVC capture.
 */
class MvcCaptureFilterTest {
    @Test
    void capturesEnvelopeWithCoreRequestAndResponseData() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

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
        assertThat(envelope.response().status()).isEqualTo(201);
        assertThat(envelope.response().headers()).containsKey("x-res");
        assertThat(envelope.response().body()).contains("\"id\":1");
        assertThat(envelope.response().durationMs()).isAtLeast(0L);
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":1}");
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void includeBodiesFalseStoresNullBodies() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        properties.getRecorder().setIncludeBodies(false);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

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
    }

    @Test
    void bodyIsTruncatedByConfiguredMaxBodySize() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        properties.getRecorder().setMaxBodySizeBytes(4);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

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
    }

    @Test
    void maxBodySizeLowerThanOneFallsBackToSingleByte() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        properties.getRecorder().setMaxBodySizeBytes(0);
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContent("XYZ".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            StreamUtils.copyToByteArray(servletRequest.getInputStream());
            servletResponse.getWriter().write("LMN");
        };

        filter.doFilter(request, response, chain);

        CaptureEnvelope envelope = recorder.lastEnvelope.get();
        assertThat(envelope.request().body()).isEqualTo("X");
        assertThat(envelope.response().body()).isEqualTo("L");
    }

    @Test
    void invalidRequestEncodingFallsBackToUtf8() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

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
        TestloomProperties properties = defaultRecorderProperties();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

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
        TestloomProperties properties = defaultRecorderProperties();
        CaptureRecorder failingRecorder = envelope -> {
            throw new RuntimeException("capture failed");
        };

        MvcCaptureFilter filter = new MvcCaptureFilter(failingRecorder, properties, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("hello");

        filter.doFilter(request, response, chain);

        assertThat(response.getContentAsString()).isEqualTo("hello");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void requestOutsideIncludePatternsIsNotRecorded() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("up");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("up");
    }

    @Test
    void excludedRequestIsNotRecorded() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        properties.getRecorder().setIncludePaths(List.of("/**"));
        properties.getRecorder().setExcludePaths(List.of("/api/internal/**"));
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("blocked");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("blocked");
    }

    @Test
    void blankPathIsNotRecorded() throws Exception {
        TestloomProperties properties = defaultRecorderProperties();
        CapturingRecorder recorder = new CapturingRecorder();
        MvcCaptureFilter filter = new MvcCaptureFilter(recorder, properties, new AntPatternMvcCapturePathMatcher());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("blank");

        filter.doFilter(request, response, chain);

        assertThat(recorder.calls.get()).isEqualTo(0);
        assertThat(response.getContentAsString()).isEqualTo("blank");
    }

    private static TestloomProperties defaultRecorderProperties() {
        TestloomProperties properties = new TestloomProperties();
        properties.getRecorder().setIncludePaths(List.of("/api/**"));
        properties.getRecorder().setExcludePaths(List.of());
        properties.getRecorder().setIncludeBodies(true);
        properties.getRecorder().setMaxBodySizeBytes(1024);
        return properties;
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
