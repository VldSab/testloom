package dev.testloom.core.config.domain.exception;

import lombok.experimental.StandardException;

/**
 * Signals that loaded config violates validation rules.
 */
@StandardException
public class TestloomConfigValidationException extends TestloomConfigException {
}
