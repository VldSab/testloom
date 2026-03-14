package dev.testloom.core.config.application.service.lint;

/**
 * Small string helpers for lint rules.
 */
final class LintStrings {
    private LintStrings() {
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
