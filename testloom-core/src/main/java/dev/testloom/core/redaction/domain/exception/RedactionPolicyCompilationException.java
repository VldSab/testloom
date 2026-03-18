package dev.testloom.core.redaction.domain.exception;

import lombok.experimental.StandardException;

/**
 * Signals invalid or non-compilable redaction policy configuration.
 */
@StandardException
public class RedactionPolicyCompilationException extends RedactionException {
}
