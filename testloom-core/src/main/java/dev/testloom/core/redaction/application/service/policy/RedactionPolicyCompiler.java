package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.application.service.policy.matcher.RuleMatcherCompiler;
import dev.testloom.core.redaction.domain.exception.RedactionPolicyCompilationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles redaction configuration into runtime policies.
 *
 * <p>Decision precedence:
 * <ol>
 *     <li>Per-target fallback decision is used when no rule matches.</li>
 *     <li>Rules are checked in order.</li>
 *     <li>First matching rule wins and overrides fallback decision.</li>
 * </ol>
 */
public final class RedactionPolicyCompiler {
    private RedactionPolicyCompiler() {
    }

    public static RedactionPolicy compile(RedactionConfig config) {
        RedactionConfig safeConfig = requireValue(config, "redaction config must not be null");
        String mask = requireValue(safeConfig.getMask(), "redaction.mask must not be null");
        RedactionAction headerDefaultAction = requireValue(
                safeConfig.getHeaderDefaultAction(),
                "redaction.header-default-action must not be null"
        );
        RedactionAction queryParamDefaultAction = requireValue(
                safeConfig.getQueryParamDefaultAction(),
                "redaction.query-param-default-action must not be null"
        );
        RedactionAction jsonFieldDefaultAction = requireValue(
                safeConfig.getJsonFieldDefaultAction(),
                "redaction.json-field-default-action must not be null"
        );

        List<RedactionRule> rulesSnapshot = snapshotRules(safeConfig.getRules());

        Map<RedactionTargetType, TargetPolicy> targetPolicies = new EnumMap<>(RedactionTargetType.class);
        targetPolicies.put(
                RedactionTargetType.HEADER,
                compileTargetPolicy(
                        RedactionTargetType.HEADER,
                        mask,
                        headerDefaultAction,
                        rulesSnapshot
                )
        );
        targetPolicies.put(
                RedactionTargetType.QUERY_PARAM,
                compileTargetPolicy(
                        RedactionTargetType.QUERY_PARAM,
                        mask,
                        queryParamDefaultAction,
                        rulesSnapshot
                )
        );
        targetPolicies.put(
                RedactionTargetType.JSON_FIELD,
                compileTargetPolicy(
                        RedactionTargetType.JSON_FIELD,
                        mask,
                        jsonFieldDefaultAction,
                        rulesSnapshot
                )
        );

        return new CompiledRedactionPolicy(Collections.unmodifiableMap(targetPolicies));
    }

    private static TargetPolicy compileTargetPolicy(
            RedactionTargetType targetType,
            String mask,
            RedactionAction fallbackAction,
            List<RedactionRule> allRules
    ) {
        List<CompiledRedactionRule> rules = new ArrayList<>();
        for (var rule : allRules) {
            // allRules contains mixed target types; this compiler pass selects only the current one.
            if (rule.getType() != targetType) {
                continue;
            }
            try {
                rules.add(new CompiledRedactionRule(
                        rule.getAction(),
                        rule.getReplacement(),
                        RuleMatcherCompiler.compile(rule.getMatcher(), rule.getTarget())
                ));
            } catch (RedactionPolicyCompilationException exception) {
                throw new RedactionPolicyCompilationException(
                        "Failed to compile redaction rule '" + rule
                                + "' for targetType " + targetType,
                        exception
                );
            }
        }
        return new TargetPolicy(mask, toDecision(fallbackAction, mask), List.copyOf(rules));
    }

    private static List<RedactionRule> snapshotRules(List<RedactionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(rules));
    }

    private static RedactionDecision toDecision(RedactionAction action, String mask) {
        return switch (action) {
            case KEEP -> RedactionDecision.keep();
            case REMOVE -> RedactionDecision.remove();
            case MASK -> RedactionDecision.mask(mask);
        };
    }

    private static <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new RedactionPolicyCompilationException(message);
        }
        return value;
    }
}
