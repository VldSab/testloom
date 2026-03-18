package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionTargetType;

/**
 * Compiled redaction policy contract.
 */
public interface RedactionPolicy {
    /**
     * Resolves redaction decision for one candidate name.
     *
     * @param targetType redaction target type
     * @param candidate candidate name (header/query/json key)
     * @return resolved decision
     */
    RedactionDecision resolve(RedactionTargetType targetType, String candidate);
}
