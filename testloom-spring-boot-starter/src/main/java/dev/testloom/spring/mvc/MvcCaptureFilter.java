package dev.testloom.spring.mvc;

import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;

/**
 * Captures servlet HTTP exchanges and delegates persistence to a {@link CaptureRecorder}.
 *
 * <p>This filter is transport-adapter code for the MVC stack. It does not own
 * capture persistence, naming, or sanitization policy.
 */
public final class MvcCaptureFilter extends OncePerRequestFilter {
    private static final Log log = LogFactory.getLog(MvcCaptureFilter.class);

    private final CaptureRecorder captureRecorder;
    private final TestloomConfig config;
    private final MvcCapturePathMatcher pathMatcher;
    private final MvcCaptureEnvelopeFactory envelopeFactory;

    /**
     * Creates an MVC capture filter.
     *
     * @param captureRecorder destination recorder for captured envelopes
     * @param config          loaded Testloom config
     * @param pathMatcher     include/exclude request path matcher
     */
    public MvcCaptureFilter(CaptureRecorder captureRecorder,
                            TestloomConfig config,
                            MvcCapturePathMatcher pathMatcher) {
        this(captureRecorder, config, pathMatcher,
                new MvcCaptureEnvelopeFactory(config, Clock.systemUTC()));
    }

    /**
     * Creates an MVC capture filter with explicit envelope factory.
     *
     * @param captureRecorder destination recorder for captured envelopes
     * @param config          loaded Testloom config
     * @param pathMatcher     include/exclude request path matcher
     * @param envelopeFactory envelope builder for wrapped exchanges
     */
    public MvcCaptureFilter(CaptureRecorder captureRecorder,
                            TestloomConfig config,
                            MvcCapturePathMatcher pathMatcher,
                            MvcCaptureEnvelopeFactory envelopeFactory) {
        this.captureRecorder = Objects.requireNonNull(captureRecorder, "captureRecorder must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.pathMatcher = Objects.requireNonNull(pathMatcher, "pathMatcher must not be null");
        this.envelopeFactory = Objects.requireNonNull(envelopeFactory, "envelopeFactory must not be null");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        RecorderConfig recorderConfig = recorderConfig();
        if (recorderConfig.isIncludeBodies()) {
            doFilterWithBodies(request, response, filterChain);
            return;
        }
        doFilterWithoutBodies(request, response, filterChain);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        RecorderConfig recorderConfig = recorderConfig();
        if (!recorderConfig.isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(ServletRequestPaths.applicationPath(request))) {
            return true;
        }
        return !pathMatcher.shouldCapture(request, config);
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            return wrapper;
        }
        RecorderConfig recorderConfig = recorderConfig();
        return new ContentCachingRequestWrapper(request, recorderConfig.getMaxBodySizeBytes());
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private void doFilterWithBodies(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = wrapRequest(request);
        ContentCachingResponseWrapper wrappedResponse = wrapResponse(response);
        long startedAt = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            CaptureEnvelope envelope = envelopeFactory.create(wrappedRequest, wrappedResponse, durationMs);
            recordSafely(envelope);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void doFilterWithoutBodies(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            CaptureEnvelope envelope = envelopeFactory.createWithoutBodies(request, response, durationMs);
            recordSafely(envelope);
        }
    }

    private void recordSafely(CaptureEnvelope envelope) {
        try {
            captureRecorder.record(envelope);
        } catch (Exception exception) {
            log.warn("Unexpected capture recorder failure. Request processing will continue.", exception);
        }
    }

    private RecorderConfig recorderConfig() {
        RecorderConfig recorder = config.getRecorder();
        if (recorder == null) {
            throw new IllegalStateException("testloom.recorder must not be null");
        }
        return recorder;
    }
}
