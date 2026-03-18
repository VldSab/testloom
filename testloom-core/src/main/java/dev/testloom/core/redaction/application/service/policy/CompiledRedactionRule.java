package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.redaction.application.service.policy.matcher.RuleMatcherCompiler;

import java.util.Optional;

/**
 * Precompiled rule entry for one redaction target type.
 *
 * <p>The object stores only runtime-relevant data extracted from one config rule:
 * action, optional replacement, and compiled matcher.
 *
 * <p>Example:
 * config rule {@code type=QUERY_PARAM, target=token, action=MASK, replacement=***}
 * becomes one compiled rule where matcher checks query name {@code token}.
 */
final class CompiledRedactionRule {
    private final RedactionAction action;
    private final String replacement;
    private final RuleMatcherCompiler.RuleMatcher matcher;

    CompiledRedactionRule(RedactionAction action, String replacement, RuleMatcherCompiler.RuleMatcher matcher) {
        this.action = action;
        this.replacement = replacement;
        this.matcher = matcher;
    }

    /**
     * Tries to resolve decision for one candidate.
     *
     * <p>Returns empty when rule does not match to keep first-match-wins
     * iteration explicit in {@link TargetPolicy#resolve(String, dev.testloom.core.config.domain.model.RedactionTargetType)}.
     *
     * <p>Example:
     * for candidate {@code token}, MASK rule with replacement {@code safe}
     * returns {@code Mask("safe")}.
     */
    Optional<RedactionDecision> tryResolve(String candidate, String defaultMask) {
        if (!matcher.matches(candidate)) {
            return Optional.empty();
        }
        return Optional.of(switch (action) {
            case KEEP -> RedactionDecision.keep();
            case REMOVE -> RedactionDecision.remove();
            case MASK -> RedactionDecision.mask(replacement == null ? defaultMask : replacement);
        });
    }
}
