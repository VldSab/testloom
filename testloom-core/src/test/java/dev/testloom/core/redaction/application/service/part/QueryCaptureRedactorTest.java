package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryCaptureRedactorTest {
    @Test
    void constructorRejectsNullPolicy() {
        assertThrows(NullPointerException.class, () -> new QueryCaptureRedactor(null));
    }

    @Test
    void nullAndBlankQueryAreReturnedAsIs() {
        QueryCaptureRedactor redactor = new QueryCaptureRedactor((type, candidate) -> RedactionDecision.keep());

        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("")).isEmpty();
        assertThat(redactor.redact("  ")).isEqualTo("  ");
    }

    @Test
    void keepMaskAndRemoveAreApplied() {
        RedactionPolicy policy = (type, candidate) -> {
            if (type != RedactionTargetType.QUERY_PARAM) {
                return RedactionDecision.keep();
            }
            return switch (candidate) {
                case "token" -> RedactionDecision.mask("***");
                case "drop" -> RedactionDecision.remove();
                default -> RedactionDecision.keep();
            };
        };
        QueryCaptureRedactor redactor = new QueryCaptureRedactor(policy);

        String redacted = redactor.redact("token=abc&drop=1&page=2&&");

        assertThat(redacted).isEqualTo("token=***&page=2&&");
    }

    @Test
    void maskForFlagStyleParamPreservesOriginalShape() {
        QueryCaptureRedactor redactor = new QueryCaptureRedactor((type, candidate) -> RedactionDecision.mask("***"));

        assertThat(redactor.redact("token")).isEqualTo("token");
    }

    @Test
    void removingAllParamsReturnsNull() {
        QueryCaptureRedactor redactor = new QueryCaptureRedactor((type, candidate) -> RedactionDecision.remove());

        assertThat(redactor.redact("a=1&b=2")).isNull();
    }

    @Test
    void nullDecisionThrowsRedactionException() {
        QueryCaptureRedactor redactor = new QueryCaptureRedactor((type, candidate) -> null);

        RedactionException error = assertThrows(
                RedactionException.class,
                () -> redactor.redact("token=abc")
        );

        assertThat(error).hasMessageThat().contains("Unsupported redaction decision: null");
    }
}
