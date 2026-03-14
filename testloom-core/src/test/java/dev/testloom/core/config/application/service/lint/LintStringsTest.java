package dev.testloom.core.config.application.service.lint;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests string helper behavior used by linters.
 */
class LintStringsTest {
    @Test
    void isBlankHandlesNullEmptyAndWhitespace() {
        assertThat(LintStrings.isBlank(null)).isTrue();
        assertThat(LintStrings.isBlank("")).isTrue();
        assertThat(LintStrings.isBlank("   ")).isTrue();
        assertThat(LintStrings.isBlank("x")).isFalse();
    }
}
