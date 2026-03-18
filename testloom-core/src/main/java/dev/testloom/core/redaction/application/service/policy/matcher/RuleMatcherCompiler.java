package dev.testloom.core.redaction.application.service.policy.matcher;

import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.redaction.domain.exception.RedactionPolicyCompilationException;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles rule matcher from config matcher type and target.
 */
public final class RuleMatcherCompiler {
    private RuleMatcherCompiler() {
    }

    public static RuleMatcher compile(RedactionMatcherType matcherType, String target) {
        if (matcherType == null) {
            throw new RedactionPolicyCompilationException("redaction rule matcherType must not be null");
        }
        if (target == null) {
            throw new RedactionPolicyCompilationException("redaction rule target must not be null");
        }
        return switch (matcherType) {
            case EXACT -> candidate -> Objects.equals(candidate, target);
            case GLOB -> {
                Pattern pattern = compileRegex(globToRegex(target), "Invalid generated regex for GLOB target");
                yield candidate -> candidate != null && pattern.matcher(candidate).matches();
            }
            case REGEX -> {
                Pattern pattern = compileRegex(target, "Invalid REGEX target");
                yield candidate -> candidate != null && pattern.matcher(candidate).matches();
            }
        };
    }

    private static Pattern compileRegex(String pattern, String message) {
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException exception) {
            throw new RedactionPolicyCompilationException(message + ": " + pattern, exception);
        }
    }

    /**
     * Converts Ant-like glob mask to full-string Java regex.
     */
    static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char ch = glob.charAt(i);
            switch (ch) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.', '(', ')', '+', '|', '^', '$', '{', '}', '[', ']', '\\' -> regex.append('\\').append(ch);
                default -> regex.append(ch);
            }
        }
        regex.append('$');
        return regex.toString();
    }

    @FunctionalInterface
    public interface RuleMatcher {
        boolean matches(String candidate);
    }
}
