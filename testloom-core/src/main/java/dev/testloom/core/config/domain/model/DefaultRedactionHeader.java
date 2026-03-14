package dev.testloom.core.config.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Built-in default header keys that should be redacted.
 */
public enum DefaultRedactionHeader {
    AUTHORIZATION("authorization"),
    COOKIE("cookie"),
    SET_COOKIE("set-cookie");

    private final String value;

    DefaultRedactionHeader(String value) {
        this.value = value;
    }

    /**
     * Returns default values as immutable list.
     *
     * @return header keys
     */
    public static List<String> valuesList() {
        return Arrays.stream(values()).map(DefaultRedactionHeader::value).toList();
    }

    public String value() {
        return value;
    }
}
