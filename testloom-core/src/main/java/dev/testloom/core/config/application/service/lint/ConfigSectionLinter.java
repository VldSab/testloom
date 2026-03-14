package dev.testloom.core.config.application.service.lint;

import java.util.List;

/**
 * Lints one config section and appends errors to a shared list.
 *
 * @param <T> section type
 */
@FunctionalInterface
public interface ConfigSectionLinter<T> {
    /**
     * Appends lint errors for provided section.
     *
     * @param section section value
     * @param errors collected lint errors
     */
    void lint(T section, List<String> errors);
}
