package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests redaction section linting rules.
 */
class RedactionConfigLinterTest {
    private final RedactionConfigLinter linter = new RedactionConfigLinter();

    @Test
    void nullRedactionProducesError() {
        List<String> errors = new ArrayList<>();

        linter.lint(null, errors);

        assertThat(errors).containsExactly("testloom.redaction must not be null.");
    }

    @Test
    void invalidRedactionProducesExpectedErrors() {
        RedactionRule invalidRule = new RedactionRule();
        invalidRule.setTarget("  ");

        RedactionRule invalidRegexRule = new RedactionRule();
        invalidRegexRule.setType(RedactionTargetType.HEADER);
        invalidRegexRule.setMatcher(RedactionMatcherType.REGEX);
        invalidRegexRule.setAction(RedactionAction.MASK);
        invalidRegexRule.setTarget("[");

        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask(" ");
        redaction.setRules(new ArrayList<>(Arrays.asList(null, invalidRule, invalidRegexRule)));

        List<String> errors = new ArrayList<>();
        linter.lint(redaction, errors);

        assertThat(errors).contains("testloom.redaction.mask must not be blank.");
        assertThat(errors).contains("testloom.redaction.rules[0] must not be null.");
        assertThat(errors).contains("testloom.redaction.rules[1].type must not be null.");
        assertThat(errors).contains("testloom.redaction.rules[1].matcher must not be null.");
        assertThat(errors).contains("testloom.redaction.rules[1].action must not be null.");
        assertThat(errors).contains("testloom.redaction.rules[1].target must not be blank.");
        assertThat(errors.stream().anyMatch(m -> m.contains("testloom.redaction.rules[2].target is not a valid regex"))).isTrue();
    }

    @Test
    void validRegexRuleDoesNotProduceError() {
        RedactionRule regexRule = new RedactionRule();
        regexRule.setType(RedactionTargetType.HEADER);
        regexRule.setMatcher(RedactionMatcherType.REGEX);
        regexRule.setAction(RedactionAction.MASK);
        regexRule.setTarget("^[a-z-]+$");

        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setRules(List.of(regexRule));

        List<String> errors = new ArrayList<>();
        linter.lint(redaction, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void nonRegexMatcherDoesNotTriggerRegexCompilation() {
        RedactionRule exactRule = new RedactionRule();
        exactRule.setType(RedactionTargetType.HEADER);
        exactRule.setMatcher(RedactionMatcherType.EXACT);
        exactRule.setAction(RedactionAction.MASK);
        exactRule.setTarget("[");

        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setRules(List.of(exactRule));

        List<String> errors = new ArrayList<>();
        linter.lint(redaction, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void nullRulesCollectionIsAllowed() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setRules(null);

        List<String> errors = new ArrayList<>();
        linter.lint(redaction, errors);

        assertThat(errors).isEmpty();
    }
}
