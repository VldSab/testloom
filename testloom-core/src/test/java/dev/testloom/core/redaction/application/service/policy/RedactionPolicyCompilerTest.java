package dev.testloom.core.redaction.application.service.policy;

import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.domain.exception.RedactionPolicyCompilationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedactionPolicyCompilerTest {
    @Test
    void compileRejectsMissingRequiredFields() {
        assertThrows(RedactionPolicyCompilationException.class, () -> RedactionPolicyCompiler.compile(null));

        RedactionConfig config = new RedactionConfig();
        config.setMask("***");
        config.setHeaderDefaultAction(RedactionAction.MASK);
        config.setQueryParamDefaultAction(RedactionAction.MASK);
        config.setJsonFieldDefaultAction(RedactionAction.MASK);

        config.setMask(null);
        assertThrows(RedactionPolicyCompilationException.class, () -> RedactionPolicyCompiler.compile(config));

        config.setMask("***");
        config.setHeaderDefaultAction(null);
        assertThrows(RedactionPolicyCompilationException.class, () -> RedactionPolicyCompiler.compile(config));

        config.setHeaderDefaultAction(RedactionAction.MASK);
        config.setQueryParamDefaultAction(null);
        assertThrows(RedactionPolicyCompilationException.class, () -> RedactionPolicyCompiler.compile(config));

        config.setQueryParamDefaultAction(RedactionAction.MASK);
        config.setJsonFieldDefaultAction(null);
        assertThrows(RedactionPolicyCompilationException.class, () -> RedactionPolicyCompiler.compile(config));
    }

    @Test
    void compileHandlesNullAndEmptyRulesByUsingFallback() {
        RedactionConfig nullRulesConfig = baseConfig();
        nullRulesConfig.setRules(null);
        RedactionPolicy nullRulesPolicy = RedactionPolicyCompiler.compile(nullRulesConfig);
        assertThat(nullRulesPolicy.resolve(RedactionTargetType.HEADER, "authorization"))
                .isEqualTo(RedactionDecision.mask("***"));

        RedactionConfig emptyRulesConfig = baseConfig();
        RedactionPolicy emptyRulesPolicy = RedactionPolicyCompiler.compile(emptyRulesConfig);
        assertThat(emptyRulesPolicy.resolve(RedactionTargetType.QUERY_PARAM, "token"))
                .isEqualTo(RedactionDecision.mask("***"));
    }

    @Test
    void compileWrapsRuleCompilationErrors() {
        RedactionConfig config = baseConfig();
        RedactionRule invalidRegexRule = new RedactionRule();
        invalidRegexRule.setType(RedactionTargetType.HEADER);
        invalidRegexRule.setMatcher(RedactionMatcherType.REGEX);
        invalidRegexRule.setAction(RedactionAction.MASK);
        invalidRegexRule.setTarget("[");
        config.setRules(java.util.List.of(invalidRegexRule));

        RedactionPolicyCompilationException error = assertThrows(
                RedactionPolicyCompilationException.class,
                () -> RedactionPolicyCompiler.compile(config)
        );

        assertThat(error).hasMessageThat().contains("Failed to compile redaction rule");
        assertThat(error.getCause()).isInstanceOf(RedactionPolicyCompilationException.class);
        assertThat(error.getCause()).hasMessageThat().contains("Invalid REGEX target");
    }

    @Test
    void compiledPolicyReturnsKeepForMissingTargetPolicy() {
        CompiledRedactionPolicy policy = new CompiledRedactionPolicy(Map.of());

        assertThat(policy.resolve(RedactionTargetType.HEADER, "authorization"))
                .isEqualTo(RedactionDecision.keep());
    }

    @Test
    void compiledPolicyUsesFirstMatchingRuleAndFallback() {
        RedactionConfig config = baseConfig();
        RedactionRule first = new RedactionRule();
        first.setType(RedactionTargetType.QUERY_PARAM);
        first.setMatcher(RedactionMatcherType.EXACT);
        first.setTarget("token");
        first.setAction(RedactionAction.MASK);
        first.setReplacement("safe");

        RedactionRule second = new RedactionRule();
        second.setType(RedactionTargetType.QUERY_PARAM);
        second.setMatcher(RedactionMatcherType.EXACT);
        second.setTarget("token");
        second.setAction(RedactionAction.REMOVE);

        config.setRules(java.util.List.of(first, second));
        RedactionPolicy policy = RedactionPolicyCompiler.compile(config);

        assertThat(policy.resolve(RedactionTargetType.QUERY_PARAM, "token"))
                .isEqualTo(RedactionDecision.mask("safe"));
        assertThat(policy.resolve(RedactionTargetType.JSON_FIELD, " "))
                .isEqualTo(RedactionDecision.mask("***"));
    }

    private static RedactionConfig baseConfig() {
        RedactionConfig config = new RedactionConfig();
        config.setMask("***");
        config.setHeaderDefaultAction(RedactionAction.MASK);
        config.setQueryParamDefaultAction(RedactionAction.MASK);
        config.setJsonFieldDefaultAction(RedactionAction.MASK);
        config.setRules(java.util.List.of());
        return config;
    }
}
