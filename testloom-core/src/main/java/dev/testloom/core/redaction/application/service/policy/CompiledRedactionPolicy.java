package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionTargetType;

import java.util.Map;

/**
 * Runtime map-backed implementation of compiled redaction policy.
 */
final class CompiledRedactionPolicy implements RedactionPolicy {
    private final Map<RedactionTargetType, TargetPolicy> policies;

    CompiledRedactionPolicy(Map<RedactionTargetType, TargetPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public RedactionDecision resolve(RedactionTargetType targetType, String candidate) {
        TargetPolicy policy = policies.get(targetType);
        if (policy == null) {
            return RedactionDecision.keep();
        }
        return policy.resolve(candidate, targetType);
    }
}
