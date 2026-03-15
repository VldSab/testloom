package dev.testloom.spring.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Utility methods for deriving stable application paths from servlet requests.
 */
final class ServletRequestPaths {
    private ServletRequestPaths() {
    }

    /**
     * Returns request URI without servlet context path prefix.
     *
     * @param request servlet request
     * @return application path or empty string if URI is unavailable
     */
    static String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (!StringUtils.hasText(contextPath)) {
            return uri;
        }
        if (!uri.startsWith(contextPath)) {
            return uri;
        }
        String stripped = uri.substring(contextPath.length());
        return stripped.isEmpty() ? "/" : stripped;
    }
}
