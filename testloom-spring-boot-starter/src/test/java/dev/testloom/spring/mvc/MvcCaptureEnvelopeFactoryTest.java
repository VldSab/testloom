package dev.testloom.spring.mvc;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests envelope factory behavior not covered through filter-level tests.
 */
class MvcCaptureEnvelopeFactoryTest {
    @Test
    void createUsesUtf8WhenRequestEncodingIsBlank() throws Exception {
        TestloomConfig config = defaultConfig();
        MvcCaptureEnvelopeFactory factory = new MvcCaptureEnvelopeFactory(config, fixedClock());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setCharacterEncoding("");
        request.setContent("привет".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024);
        StreamUtils.copyToByteArray(wrappedRequest.getInputStream());

        MockHttpServletResponse response = new MockHttpServletResponse();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        wrappedResponse.getWriter().write("ok");

        CaptureEnvelope envelope = factory.create(wrappedRequest, wrappedResponse, 12);

        assertThat(envelope.request().body()).isEqualTo("привет");
    }

    @Test
    void createWithoutBodiesReadsNumericContentLengthHeader() {
        TestloomConfig config = defaultConfig();
        MvcCaptureEnvelopeFactory factory = new MvcCaptureEnvelopeFactory(config, fixedClock());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Content-Length", "7");

        CaptureEnvelope envelope = factory.createWithoutBodies(request, response, 3);

        assertThat(envelope.response().truncation().originalSizeBytes()).isEqualTo(7);
        assertThat(envelope.response().truncation().capturedSizeBytes()).isEqualTo(0);
    }

    @Test
    void createWithoutBodiesFallsBackWhenContentLengthIsInvalid() {
        TestloomConfig config = defaultConfig();
        MvcCaptureEnvelopeFactory factory = new MvcCaptureEnvelopeFactory(config, fixedClock());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getHeader("Content-Length")).thenReturn("bad");
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaderNames()).thenReturn(Collections.emptyList());
        when(response.getContentType()).thenReturn(null);

        CaptureEnvelope envelope = factory.createWithoutBodies(request, response, 3);

        assertThat(envelope.response().truncation().originalSizeBytes()).isEqualTo(0);
    }

    @Test
    void missingRecorderConfigFailsFast() {
        TestloomConfig config = new TestloomConfig();
        MvcCaptureEnvelopeFactory factory = new MvcCaptureEnvelopeFactory(config, fixedClock());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> factory.create(wrappedRequest, wrappedResponse, 1)
        );

        assertThat(error).hasMessageThat().contains("testloom.recorder must not be null");
    }

    private static TestloomConfig defaultConfig() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setEnabled(true);
        config.getRecorder().setIncludeBodies(true);
        config.getRecorder().setMaxBodySizeBytes(1024);
        return config;
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC);
    }
}
