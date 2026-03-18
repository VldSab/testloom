package dev.testloom.core.redaction.application.service.policy;

import java.util.Objects;

/**
 * Decision returned by redaction policy for a single candidate value.
 */
public sealed interface RedactionDecision permits RedactionDecision.Keep, RedactionDecision.Remove, RedactionDecision.Mask {
    Keep KEEP = new Keep();
    Remove REMOVE = new Remove();

    static RedactionDecision keep() {
        return KEEP;
    }

    static RedactionDecision remove() {
        return REMOVE;
    }

    static RedactionDecision mask(String replacement) {
        return new Mask(Objects.requireNonNull(replacement, "replacement must not be null"));
    }

    /**
     * Keep original value unchanged.
     */
    final class Keep implements RedactionDecision {
        private Keep() {
        }
    }

    /**
     * Remove value completely.
     */
    final class Remove implements RedactionDecision {
        private Remove() {
        }
    }

    /**
     * Replace value with provided mask string.
     */
    record Mask(String replacement) implements RedactionDecision {
        public Mask {
            Objects.requireNonNull(replacement, "replacement must not be null");
        }
    }
}
