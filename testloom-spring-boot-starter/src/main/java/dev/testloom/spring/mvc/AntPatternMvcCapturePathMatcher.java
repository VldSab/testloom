package dev.testloom.spring.mvc;

import dev.testloom.spring.properties.TestloomProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * MVC path matcher implementation based on Spring Ant-style patterns.
 */
public final class AntPatternMvcCapturePathMatcher implements MvcCapturePathMatcher {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean shouldCapture(HttpServletRequest request, TestloomProperties.RecorderProperties recorder) {
        String path = ServletRequestPaths.applicationPath(request);
        if (!StringUtils.hasText(path)) {
            return false;
        }
        if (!matchesAny(path, recorder.getIncludePaths())) {
            return false;
        }
        return !matchesAny(path, recorder.getExcludePaths());
    }

    private boolean matchesAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
