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
    @JsonProperty("header-default-action")
    private RedactionAction headerDefaultAction;
    @JsonProperty("json-field-default-action")
    private RedactionAction jsonFieldDefaultAction;
    @JsonProperty("query-param-default-action")
    private RedactionAction queryParamDefaultAction;
    private List<RedactionRule> rules;

    /**
     * Returns default redaction settings.
     *
     * @return default redaction config
     */
    public static RedactionConfig defaults() {
        RedactionConfig config = new RedactionConfig();
        config.setMask("***");
        config.setHeaderDefaultAction(RedactionAction.MASK);
        config.setJsonFieldDefaultAction(RedactionAction.MASK);
        config.setQueryParamDefaultAction(RedactionAction.MASK);
        config.setRules(List.of());
        return config;
    }
}
