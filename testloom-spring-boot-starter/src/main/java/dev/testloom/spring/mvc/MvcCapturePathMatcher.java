package dev.testloom.spring.mvc;

import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Matches servlet request paths against recorder include/exclude rules.
 */
public interface MvcCapturePathMatcher {
    /**
     * Determines whether the request should be captured.
     *
     * @param request current servlet request
     * @param config loaded Testloom config
     * @return {@code true} when the request should be captured
     *
     * <p>Path-matching implementations are expected to be fail-closed: include patterns
     * must match first, then exclude patterns take precedence and suppress capture.
     * Empty or invalid patterns should be treated as non-matches.
     * @throws NullPointerException when request, config, or mandatory config sections are missing
     */
    boolean shouldCapture(HttpServletRequest request, TestloomConfig config);
}
