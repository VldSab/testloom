package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionNameNormalizer;
import dev.testloom.core.config.domain.model.RedactionTargetType;

import java.util.List;
import java.util.Optional;

/**
 * Target-type-specific policy containing fallback decision and ordered compiled rules.
 */
final class TargetPolicy {
    private final String mask;
    private final RedactionDecision fallbackDecision;
    private final List<CompiledRedactionRule> rules;

    TargetPolicy(String mask, RedactionDecision fallbackDecision, List<CompiledRedactionRule> rules) {
        this.mask = mask;
        this.fallbackDecision = fallbackDecision;
        this.rules = rules;
    }

    /**
     * Resolves effective redaction decision for one incoming name.
     *
     * <p>Flow:
     * <ol>
     *     <li>Normalize incoming name for target type.</li>
     *     <li>Apply first matching compiled rule.</li>
     *     <li>If no rules matched, return fallback decision.</li>
     * </ol>
     */
    RedactionDecision resolve(String candidate, RedactionTargetType targetType) {
        String normalizedCandidate = RedactionNameNormalizer.normalize(candidate, targetType);
        if (normalizedCandidate == null) {
            return fallbackDecision;
        }

        for (CompiledRedactionRule rule : rules) {
            Optional<RedactionDecision> decision = rule.tryResolve(normalizedCandidate, mask);
            if (decision.isPresent()) {
                return decision.get();
            }
        }
        return fallbackDecision;
    }
}
