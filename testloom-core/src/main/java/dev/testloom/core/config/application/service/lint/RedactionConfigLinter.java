package dev.testloom.core.config.application.service.lint;

import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.config.domain.model.RedactionRule;

import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Lints redaction-specific config values and rules.
 */
public final class RedactionConfigLinter implements ConfigSectionLinter<RedactionConfig> {
    @Override
    public void lint(RedactionConfig redaction, List<String> errors) {
        if (redaction == null) {
            errors.add("testloom.redaction must not be null.");
            return;
        }
        if (LintStrings.isBlank(redaction.getMask())) {
            errors.add("testloom.redaction.mask must not be blank.");
        }
        if (redaction.getHeaderDefaultAction() == null) {
            errors.add("testloom.redaction.header-default-action must not be null.");
        }
        if (redaction.getQueryParamDefaultAction() == null) {
            errors.add("testloom.redaction.query-param-default-action must not be null.");
        }
        if (redaction.getJsonFieldDefaultAction() == null) {
            errors.add("testloom.redaction.json-field-default-action must not be null.");
        }
        lintRules(redaction.getRules(), errors);
    }

    private void lintRules(List<RedactionRule> rules, List<String> errors) {
        if (rules == null) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            lintRule(rules.get(i), i, errors);
        }
    }

    private void lintRule(RedactionRule rule, int index, List<String> errors) {
        if (rule == null) {
            errors.add("testloom.redaction.rules[" + index + "] must not be null.");
            return;
        }
        if (rule.getType() == null) {
            errors.add("testloom.redaction.rules[" + index + "].type must not be null.");
        }
        if (rule.getMatcher() == null) {
            errors.add("testloom.redaction.rules[" + index + "].matcher must not be null.");
        }
        if (rule.getAction() == null) {
            errors.add("testloom.redaction.rules[" + index + "].action must not be null.");
        }
        if (LintStrings.isBlank(rule.getTarget())) {
            errors.add("testloom.redaction.rules[" + index + "].target must not be blank.");
        }
        if (rule.getMatcher() == RedactionMatcherType.REGEX && !LintStrings.isBlank(rule.getTarget())) {
            lintRegexTarget(rule.getTarget(), index, errors);
        }
    }

    private void lintRegexTarget(String value, int ruleIndex, List<String> errors) {
        try {
            java.util.regex.Pattern.compile(value);
        } catch (PatternSyntaxException ex) {
            errors.add("testloom.redaction.rules[" + ruleIndex + "].target is not a valid regex: " + ex.getDescription());
        }
    }

}
