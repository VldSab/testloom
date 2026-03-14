package dev.testloom.core.config.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Redaction settings and rule list loaded from configuration.
 */
@Getter
@Setter
public class RedactionConfig {
    private String mask;
    private List<String> headers;
    @JsonProperty("json-fields")
    private List<String> jsonFields;
    @JsonProperty("query-params")
    private List<String> queryParams;
    private List<RedactionRule> rules;

    /**
     * Returns default redaction settings.
     *
     * @return default redaction config
     */
    public static RedactionConfig defaults() {
        RedactionConfig config = new RedactionConfig();
        config.setMask("***");
        config.setHeaders(DefaultRedactionHeader.valuesList());
        config.setJsonFields(DefaultRedactionJsonField.valuesList());
        config.setQueryParams(DefaultRedactionQueryParam.valuesList());
        config.setRules(List.of());
        return config;
    }
}
