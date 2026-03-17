package dev.testloom.spring.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extracts request and response headers into immutable maps.
 */
final class MvcHeaderExtractor {
    /**
     * Extracts request headers.
     *
     * @param request servlet request
     * @return immutable headers map
     */
    Map<String, List<String>> requestHeaders(HttpServletRequest request) {
        return extract(
                Collections.list(request.getHeaderNames()),
                name -> Collections.list(request.getHeaders(name))
        );
    }

    /**
     * Extracts response headers.
     *
     * @param response servlet response
     * @return immutable headers map
     */
    Map<String, List<String>> responseHeaders(HttpServletResponse response) {
        return extract(
                response.getHeaderNames(),
                name -> new ArrayList<>(response.getHeaders(name))
        );
    }

    private Map<String, List<String>> extract(Iterable<String> names,
                                              Function<String, List<String>> valuesExtractor) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : names) {
            headers.put(name, List.copyOf(valuesExtractor.apply(name)));
        }
        return headers;
    }
}
