package dev.testloom.spring.mvc;

import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.TestloomConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * MVC path matcher implementation based on Spring Ant-style patterns.
 */
@Slf4j
public final class AntPatternMvcCapturePathMatcher implements MvcCapturePathMatcher {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean shouldCapture(HttpServletRequest request, TestloomConfig config) {
        Objects.requireNonNull(request, "request must not be null");
        TestloomConfig safeConfig = Objects.requireNonNull(config, "config must not be null");
        RecorderConfig recorder = Objects.requireNonNull(safeConfig.getRecorder(), "testloom.recorder must not be null");
        String path = ServletRequestPaths.applicationPath(request);
        if (!StringUtils.hasText(path)) {
            return false;
        }
        if (!matchesAny(path, recorder.getIncludePaths())) {
            return false;
        }
        return !matchesAny(path, recorder.getExcludePaths());
    }

    /**
     * Returns {@code true} when at least one pattern matches the path.
     *
     * <p>A {@code null}, empty, blank, or invalid pattern entry is treated as "no match"
     * to keep filtering fail-closed and avoid throwing from request path checks.
     */
    private boolean matchesAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            try {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                log.debug("Invalid capture path pattern '{}' for request path '{}'; treated as non-match.", pattern, path);
            }
        }
        return false;
    }
}
