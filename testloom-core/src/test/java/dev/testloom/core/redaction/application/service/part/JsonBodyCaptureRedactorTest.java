package dev.testloom.core.redaction.application.service.part;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.redaction.application.service.policy.RedactionDecision;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;
import dev.testloom.core.redaction.domain.exception.RedactionException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonBodyCaptureRedactorTest {
    @Test
    void constructorRejectsNullArguments() {
        RedactionPolicy keepPolicy = (type, candidate) -> RedactionDecision.keep();
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(NullPointerException.class, () -> new JsonBodyCaptureRedactor(null, keepPolicy));
        assertThrows(NullPointerException.class, () -> new JsonBodyCaptureRedactor(objectMapper, null));
    }

    @Test
    void nullOrBlankBodyIsReturnedAsIs() {
        JsonBodyCaptureRedactor redactor = new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.mask("***"));

        assertThat(redactor.redactIfJson(null, "application/json")).isNull();
        assertThat(redactor.redactIfJson("", "application/json")).isEmpty();
        assertThat(redactor.redactIfJson(" ", "application/json")).isEqualTo(" ");
    }

    @Test
    void nonJsonBodyUsesFallbackDecision() {
        JsonBodyCaptureRedactor keepRedactor = new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.keep());
        JsonBodyCaptureRedactor maskRedactor = new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.mask("***"));
        JsonBodyCaptureRedactor removeRedactor = new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.remove());

        assertThat(keepRedactor.redactIfJson("password=plain", "text/plain")).isEqualTo("password=plain");
        assertThat(maskRedactor.redactIfJson("password=plain", "text/plain")).isEqualTo("***");
        assertThat(removeRedactor.redactIfJson("password=plain", "text/plain")).isNull();
    }

    @Test
    void jsonNodeNullFromMapperUsesFallbackDecision() {
        ObjectMapper mapperReturningNull = new ObjectMapper() {
            @Override
            public JsonNode readTree(String content) {
                return null;
            }
        };
        JsonBodyCaptureRedactor redactor =
                new JsonBodyCaptureRedactor(mapperReturningNull, (type, candidate) -> RedactionDecision.mask("***"));

        assertThat(redactor.redactIfJson("{\"x\":1}", "application/json")).isEqualTo("***");
    }

    @Test
    void parseOrWriteFailureUsesFallbackDecision() {
        JsonBodyCaptureRedactor parseFailRedactor =
                new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.mask("***"));

        assertThat(parseFailRedactor.redactIfJson("{\"x\":", "application/json")).isEqualTo("***");

        ObjectMapper writeFailMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                };
            }
        };
        JsonBodyCaptureRedactor writeFailRedactor =
                new JsonBodyCaptureRedactor(writeFailMapper, (type, candidate) -> RedactionDecision.mask("***"));

        assertThat(writeFailRedactor.redactIfJson("{\"x\":1}", "application/json")).isEqualTo("***");
    }

    @Test
    void keepMaskAndRemoveAreAppliedForJsonFields() {
        RedactionPolicy policy = (type, candidate) -> {
            if (type != RedactionTargetType.JSON_FIELD) {
                return RedactionDecision.keep();
            }
            return switch (candidate) {
                case null -> RedactionDecision.keep();
                case "secret" -> RedactionDecision.remove();
                case "token" -> RedactionDecision.mask("***");
                default -> RedactionDecision.keep();
            };
        };
        JsonBodyCaptureRedactor redactor = new JsonBodyCaptureRedactor(new ObjectMapper(), policy);

        String redacted = redactor.redactIfJson(
                "{\"token\":\"a\",\"secret\":\"b\",\"nested\":{\"token\":\"c\"},\"items\":[{\"token\":\"d\"}],\"flag\":true}",
                "application/json"
        );

        assertThat(redacted).contains("\"token\":\"***\"");
        assertThat(redacted).doesNotContain("\"secret\"");
        assertThat(redacted).contains("\"nested\":{\"token\":\"***\"}");
        assertThat(redacted).contains("\"items\":[{\"token\":\"***\"}]");
        assertThat(redacted).contains("\"flag\":true");
    }

    @Test
    void nullDecisionInFieldOrFallbackThrowsRedactionException() {
        JsonBodyCaptureRedactor nullFieldDecisionRedactor =
                new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> candidate == null
                        ? RedactionDecision.keep()
                        : null);

        RedactionException fieldError = assertThrows(
                RedactionException.class,
                () -> nullFieldDecisionRedactor.redactIfJson("{\"token\":\"x\"}", "application/json")
        );
        assertThat(fieldError).hasMessageThat().contains("Unsupported redaction decision: null");

        JsonBodyCaptureRedactor nullFallbackDecisionRedactor =
                new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> null);

        RedactionException fallbackError = assertThrows(
                RedactionException.class,
                () -> nullFallbackDecisionRedactor.redactIfJson("not-json", "text/plain")
        );
        assertThat(fallbackError).hasMessageThat().contains("Unsupported redaction decision: null");
    }

    @Test
    void redactNodeReturnsNullWhenInputNodeIsNull() throws Exception {
        JsonBodyCaptureRedactor redactor = new JsonBodyCaptureRedactor(new ObjectMapper(), (type, candidate) -> RedactionDecision.keep());
        Method redactNode = JsonBodyCaptureRedactor.class.getDeclaredMethod("redactNode", JsonNode.class);
        redactNode.setAccessible(true);

        Object result = redactNode.invoke(redactor, new Object[]{null});

        assertThat(result).isNull();
    }
}
