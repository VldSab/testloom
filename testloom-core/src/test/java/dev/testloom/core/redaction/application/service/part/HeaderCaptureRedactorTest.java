package dev.testloom.core.redaction.application.service.part;

import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeaderCaptureRedactorTest {
    @Test
    void constructorRejectsNullPolicy() {
        assertThrows(NullPointerException.class, () -> new HeaderCaptureRedactor(null));
    }

    @Test
    void nullOrEmptyHeadersReturnEmptyMap() {
        HeaderCaptureRedactor redactor = new HeaderCaptureRedactor((type, candidate) -> RedactionDecision.keep());

        assertThat(redactor.redact(null)).isEmpty();
        assertThat(redactor.redact(Map.of())).isEmpty();
    }

    @Test
    void keepMaskAndRemoveDecisionsAreApplied() {
        RedactionPolicy policy = (type, candidate) -> {
            if (type != RedactionTargetType.HEADER) {
                return RedactionDecision.keep();
            }
            return switch (candidate) {
                case "x-remove" -> RedactionDecision.remove();
                case "x-mask" -> RedactionDecision.mask("***");
                default -> RedactionDecision.keep();
            };
        };
        HeaderCaptureRedactor redactor = new HeaderCaptureRedactor(policy);

        Map<String, List<String>> result = redactor.redact(Map.of(
                "x-keep", List.of("v1"),
                "x-remove", List.of("v2"),
                "x-mask", List.of("v3", "v4")
        ));

        assertThat(result).containsKey("x-keep");
        assertThat(result).doesNotContainKey("x-remove");
        assertThat(result.get("x-mask")).containsExactly("***", "***").inOrder();
    }

    @Test
    void nullHeaderValueIsNormalizedToEmptyStringForKeepDecision() {
        HeaderCaptureRedactor redactor = new HeaderCaptureRedactor((type, candidate) -> RedactionDecision.keep());
        Map<String, List<String>> input = new LinkedHashMap<>();
        input.put("x-null", java.util.Collections.singletonList(null));

        Map<String, List<String>> result = redactor.redact(input);

        assertThat(result.get("x-null")).containsExactly("");
    }

    @Test
    void nullDecisionThrowsRedactionException() {
        HeaderCaptureRedactor redactor = new HeaderCaptureRedactor((type, candidate) -> null);

        RedactionException error = assertThrows(
                RedactionException.class,
                () -> redactor.redact(Map.of("x", List.of("1")))
        );

        assertThat(error).hasMessageThat().contains("Unsupported redaction decision: null");
    }
}
