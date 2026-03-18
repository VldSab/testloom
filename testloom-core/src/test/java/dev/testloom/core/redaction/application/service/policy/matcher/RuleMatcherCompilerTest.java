package dev.testloom.core.redaction.application.service.policy.matcher;

import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.redaction.domain.exception.RedactionPolicyCompilationException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests matcher compilation and glob conversion edge-cases.
 */
class RuleMatcherCompilerTest {
    @Test
    void globMatcherSupportsWildcardAndQuestionMark() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "x-?-id");

        assertThat(matcher.matches("x-a-id")).isTrue();
        assertThat(matcher.matches("x-ab-id")).isFalse();
    }

    @Test
    void globMatcherEscapesRegexSpecialCharacters() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "a+b(c).d");

        assertThat(matcher.matches("a+b(c).d")).isTrue();
        assertThat(matcher.matches("ab(c).d")).isFalse();
    }

    @Test
    void globMatcherHandlesBackslashesAsLiterals() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "path\\to\\*");

        assertThat(matcher.matches("path\\to\\file")).isTrue();
    }

    @Test
    void globMatcherMatchesWholeStringOnly() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "token");

        assertThat(matcher.matches("token")).isTrue();
        assertThat(matcher.matches("x-token")).isFalse();
        assertThat(matcher.matches("token-x")).isFalse();
    }

    @Test
    void globMatcherHandlesEmptyPattern() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "");

        assertThat(matcher.matches("")).isTrue();
        assertThat(matcher.matches("x")).isFalse();
    }

    @Test
    void globMatcherStarMatchesAnyString() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "*");

        assertThat(matcher.matches("")).isTrue();
        assertThat(matcher.matches("abc")).isTrue();
        assertThat(matcher.matches("тест")).isTrue();
    }

    @Test
    void globMatcherSupportsUnicodeLiterals() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "ключ-*");

        assertThat(matcher.matches("ключ-значение")).isTrue();
        assertThat(matcher.matches("key-value")).isFalse();
    }

    @Test
    void globMatcherTreatsAtAndPercentAsLiterals() {
        RuleMatcherCompiler.RuleMatcher matcher =
                RuleMatcherCompiler.compile(RedactionMatcherType.GLOB, "id@tenant%prod");

        assertThat(matcher.matches("id@tenant%prod")).isTrue();
        assertThat(matcher.matches("id@tenantXprod")).isFalse();
    }

    @Test
    void regexMatcherInvalidPatternReturnsDiagnosticError() {
        RedactionPolicyCompilationException error = assertThrows(
                RedactionPolicyCompilationException.class,
                () -> RuleMatcherCompiler.compile(RedactionMatcherType.REGEX, "[")
        );

        assertThat(error).hasMessageThat().contains("Invalid REGEX target");
    }

    @Test
    void compileRejectsNullMatcherTypeAndTarget() {
        RedactionPolicyCompilationException nullMatcher = assertThrows(
                RedactionPolicyCompilationException.class,
                () -> RuleMatcherCompiler.compile(null, "token")
        );
        assertThat(nullMatcher).hasMessageThat().contains("matcherType must not be null");

        RedactionPolicyCompilationException nullTarget = assertThrows(
                RedactionPolicyCompilationException.class,
                () -> RuleMatcherCompiler.compile(RedactionMatcherType.EXACT, null)
        );
        assertThat(nullTarget).hasMessageThat().contains("target must not be null");
    }
}
