package dev.testloom.spring.mvc;

import dev.testloom.spring.properties.TestloomProperties;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Matches servlet request paths against recorder include/exclude rules.
 */
public interface MvcCapturePathMatcher {
    /**
     * Determines whether the request should be captured.
     *
     * @param request current servlet request
     * @param recorder recorder properties
     * @return {@code true} when the request should be captured
     */
    boolean shouldCapture(HttpServletRequest request, TestloomProperties.RecorderProperties recorder);
}
