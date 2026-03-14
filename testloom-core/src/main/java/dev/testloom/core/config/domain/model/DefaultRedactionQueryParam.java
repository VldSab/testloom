package dev.testloom.core.config.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Built-in default query parameter names that should be redacted.
 */
public enum DefaultRedactionQueryParam {
    TOKEN("token"),
    API_KEY("api_key");

    private final String value;

    DefaultRedactionQueryParam(String value) {
        this.value = value;
    }

    /**
     * Returns default values as immutable list.
     *
     * @return query parameter names
     */
    public static List<String> valuesList() {
        return Arrays.stream(values()).map(DefaultRedactionQueryParam::value).toList();
    }

    public String value() {
        return value;
    }
}
