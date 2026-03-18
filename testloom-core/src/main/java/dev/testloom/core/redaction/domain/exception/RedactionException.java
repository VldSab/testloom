package dev.testloom.core.redaction.domain.exception;

import lombok.experimental.StandardException;

/**
 * Runtime exception for redaction pipeline failures.
 */
@StandardException
public class RedactionException extends RuntimeException {
}
