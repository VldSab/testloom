package dev.testloom.core.config.domain.model;

import java.util.List;

/**
 * Built-in default JSON field names that should be redacted.
 */
public enum DefaultRedactionJsonField {
    PASSWORD("password"),
    TOKEN("token"),
    SECRET("secret");

    private final String value;

    DefaultRedactionJsonField(String value) {
        this.value = value;
    }

    /**
     * Returns default values as immutable list.
     *
     * @return JSON field names
     */
    public static List<String> valuesList() {
        return java.util.Arrays.stream(values()).map(DefaultRedactionJsonField::value).toList();
    }

    public String value() {
        return value;
    }
}
