package dev.testloom.spring.mvc;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Builds capture envelopes from MVC request/response wrappers.
 */
public final class MvcCaptureEnvelopeFactory {
    private static final String SCHEMA_VERSION = "0.1.0";
    private static final String TRANSPORT = "HTTP";

    private final Clock clock;
    private final TestloomConfig config;
    private final MvcHeaderExtractor headerExtractor;
    private final MvcBodyCaptureService bodyCaptureService;

    /**
     * Creates a factory with default helper components.
     *
     * @param config loaded Testloom config
     * @param clock  timestamp source
     */
    public MvcCaptureEnvelopeFactory(TestloomConfig config, Clock clock) {
        this(config, clock, new MvcHeaderExtractor(), new MvcBodyCaptureService());
    }

    MvcCaptureEnvelopeFactory(
            TestloomConfig config,
            Clock clock,
            MvcHeaderExtractor headerExtractor,
            MvcBodyCaptureService bodyCaptureService
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.headerExtractor = Objects.requireNonNull(headerExtractor, "headerExtractor must not be null");
        this.bodyCaptureService = Objects.requireNonNull(bodyCaptureService, "bodyCaptureService must not be null");
    }

    /**
     * Builds one envelope from wrapped request and response.
     *
     * @param request    wrapped request
     * @param response   wrapped response
     * @param durationMs request duration in milliseconds
     * @return capture envelope
     */
    public CaptureEnvelope create(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long durationMs
    ) {
        RecorderConfig recorderConfig = recorderConfig();
        MvcBodyCaptureService.MvcCapturedBody requestBody = bodyCaptureService.capture(
                request.getContentAsByteArray(),
                resolveCharset(request.getCharacterEncoding()),
                request.getContentLengthLong(),
                recorderConfig.isIncludeBodies(),
                recorderConfig.getMaxBodySizeBytes()
        );
        MvcBodyCaptureService.MvcCapturedBody responseBody = bodyCaptureService.capture(
                response.getContentAsByteArray(),
                resolveCharset(response.getCharacterEncoding()),
                // Response declared length is often unavailable at this point; use cached bytes as source of truth.
                -1,
                recorderConfig.isIncludeBodies(),
                recorderConfig.getMaxBodySizeBytes()
        );

        return buildEnvelope(request, response, durationMs, requestBody, responseBody);
    }

    private RecorderConfig recorderConfig() {
        RecorderConfig recorder = config.getRecorder();
        if (recorder == null) {
            throw new IllegalStateException("testloom.recorder must not be null");
        }
        return recorder;
    }

    /**
     * Builds one envelope when body capture is disabled.
     *
     * @param request    servlet request
     * @param response   servlet response
     * @param durationMs request duration in milliseconds
     * @return capture envelope
     */
    public CaptureEnvelope createWithoutBodies(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs
    ) {
        MvcBodyCaptureService.MvcCapturedBody requestBody = bodyCaptureService.capture(
                null,
                StandardCharsets.UTF_8,
                request.getContentLengthLong(),
                false,
                0
        );
        MvcBodyCaptureService.MvcCapturedBody responseBody = bodyCaptureService.capture(
                null,
                StandardCharsets.UTF_8,
                contentLengthHeader(response),
                false,
                0
        );

        return buildEnvelope(request, response, durationMs, requestBody, responseBody);
    }

    private CaptureEnvelope buildEnvelope(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs,
            MvcBodyCaptureService.MvcCapturedBody requestBody,
            MvcBodyCaptureService.MvcCapturedBody responseBody
    ) {
        return new CaptureEnvelope(
                SCHEMA_VERSION,
                Instant.now(clock).toString(),
                TRANSPORT,
                new CaptureEnvelope.RequestCapture(
                        request.getMethod(),
                        ServletRequestPaths.applicationPath(request),
                        request.getQueryString(),
                        headerExtractor.requestHeaders(request),
                        requestBody.body(),
                        request.getContentType(),
                        requestBody.truncation()
                ),
                new CaptureEnvelope.ResponseCapture(
                        response.getStatus(),
                        headerExtractor.responseHeaders(response),
                        responseBody.body(),
                        response.getContentType(),
                        durationMs,
                        responseBody.truncation()
                )
        );
    }

    private Charset resolveCharset(String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    private long contentLengthHeader(HttpServletResponse response) {
        String value = response.getHeader("Content-Length");
        if (!StringUtils.hasText(value)) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
