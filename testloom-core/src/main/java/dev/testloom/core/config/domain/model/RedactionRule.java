package dev.testloom.core.config.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Single rule describing how a matching field should be redacted.
 */
@Getter
@Setter
public class RedactionRule {
    private RedactionTargetType type;
    private String target;
    private RedactionMatcherType matcher;
    private RedactionAction action;
    private String replacement;
}
