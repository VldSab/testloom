package dev.testloom.spring.mvc;

import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.spring.properties.TestloomProperties;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Captures servlet HTTP exchanges and delegates persistence to a {@link CaptureRecorder}.
 *
 * <p>This filter is transport-adapter code for the MVC stack. It does not own
 * capture persistence, naming, or sanitization policy.
 */
public final class MvcCaptureFilter extends OncePerRequestFilter {
    private static final Log log = LogFactory.getLog(MvcCaptureFilter.class);

    private final CaptureRecorder captureRecorder;
    private final TestloomProperties properties;
    private final MvcCapturePathMatcher pathMatcher;

    /**
     * Creates an MVC capture filter.
     *
     * @param captureRecorder destination recorder for captured envelopes
     * @param properties bound runtime properties
     * @param pathMatcher include/exclude request path matcher
     */
    public MvcCaptureFilter(
            CaptureRecorder captureRecorder,
            TestloomProperties properties,
            MvcCapturePathMatcher pathMatcher
    ) {
        this.captureRecorder = captureRecorder;
        this.properties = properties;
        this.pathMatcher = pathMatcher;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = wrapRequest(request);
        ContentCachingResponseWrapper wrappedResponse = wrapResponse(response);
        long startedAt = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            CaptureEnvelope envelope = buildEnvelope(wrappedRequest, wrappedResponse, durationMs);
            try {
                captureRecorder.record(envelope);
            } catch (Exception exception) {
                log.warn("Unexpected capture recorder failure. Request processing will continue.", exception);
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!StringUtils.hasText(ServletRequestPaths.applicationPath(request))) {
            return true;
        }
        return !pathMatcher.shouldCapture(request, properties.getRecorder());
    }

    private CaptureEnvelope buildEnvelope(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long durationMs
    ) {
        return new CaptureEnvelope(
                "0.1.0",
                Instant.now().toString(),
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        request.getMethod(),
                        ServletRequestPaths.applicationPath(request),
                        request.getQueryString(),
                        extractRequestHeaders(request),
                        body(request.getContentAsByteArray(), requestCharset(request)),
                        request.getContentType()
                ),
                new CaptureEnvelope.ResponseCapture(
                        response.getStatus(),
                        extractResponseHeaders(response),
                        body(response.getContentAsByteArray(), responseCharset(response)),
                        response.getContentType(),
                        durationMs
                )
        );
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingRequestWrapper(request, maxBodyBytes());
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private Map<String, List<String>> extractRequestHeaders(HttpServletRequest request) {
        return extractHeaders(
                Collections.list(request.getHeaderNames()),
                name -> Collections.list(request.getHeaders(name))
        );
    }

    private Map<String, List<String>> extractResponseHeaders(HttpServletResponse response) {
        return extractHeaders(
                response.getHeaderNames(),
                name -> new ArrayList<>(response.getHeaders(name))
        );
    }

    private Map<String, List<String>> extractHeaders(
            Iterable<String> names,
            Function<String, List<String>> valuesExtractor
    ) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : names) {
            headers.put(name, List.copyOf(valuesExtractor.apply(name)));
        }
        return headers;
    }

    private String body(byte[] bytes, Charset charset) {
        if (!properties.getRecorder().isIncludeBodies()) {
            return null;
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        int length = Math.min(bytes.length, maxBodyBytes());
        return new String(bytes, 0, length, charset);
    }

    private Charset requestCharset(HttpServletRequest request) {
        return safeCharset(request.getCharacterEncoding());
    }

    private Charset responseCharset(HttpServletResponse response) {
        return safeCharset(response.getCharacterEncoding());
    }

    private Charset safeCharset(String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    private int maxBodyBytes() {
        return Math.max(1, properties.getRecorder().getMaxBodySizeBytes());
    }
}
