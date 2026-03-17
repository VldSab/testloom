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
     * <p>Path-matching implementations are expected to be fail-closed:
     * if include/exclude pattern sets do not produce a match, capture must be skipped.
     * @throws NullPointerException when request, config, or mandatory config sections are missing
     */
    boolean shouldCapture(HttpServletRequest request, TestloomConfig config);
}
