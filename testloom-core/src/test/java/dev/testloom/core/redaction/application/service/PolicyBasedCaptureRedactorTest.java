package dev.testloom.core.redaction.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contract tests for policy-based capture redaction.
 */
class PolicyBasedCaptureRedactorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void constructorRejectsNullConfig() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> new PolicyBasedCaptureRedactor(objectMapper, null)
        );
        assertThat(error).hasMessageThat().contains("redactionConfig must not be null");
    }

    @Test
    void nullRequestAndResponseArePreserved() {
        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, RedactionConfig.defaults());
        CaptureEnvelope source = new CaptureEnvelope(
                "0.1.0",
                "2026-03-17T12:00:00Z",
                "HTTP",
                null,
                null
        );

        CaptureEnvelope redacted = redactor.redact(source);
        assertThat(redacted.request()).isNull();
        assertThat(redacted.response()).isNull();
    }

    @Test
    void headersAreProcessedByPolicyWithoutAllowlistFiltering() {
        RedactionConfig redaction = RedactionConfig.defaults();

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "page=1"));

        assertThat(redacted.request().headers().get("authorization")).containsExactly("***");
        assertThat(redacted.request().headers().get("content-type")).containsExactly("***");
        assertThat(redacted.request().headers().get("x-secret")).containsExactly("***");
        assertThat(redacted.request().headers().get("cookie")).containsExactly("***");
        assertThat(redacted.response().headers().get("content-type")).containsExactly("***");
        assertThat(redacted.response().headers().get("set-cookie")).containsExactly("***");
    }

    @Test
    void targetFallbackActionsCanBeConfiguredWithoutRules() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.KEEP);
        redaction.setQueryParamDefaultAction(RedactionAction.REMOVE);
        redaction.setJsonFieldDefaultAction(RedactionAction.MASK);
        redaction.setRules(List.of());

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"count\":42,\"safe\":true}", "token=abc&page=1"));

        assertThat(redacted.request().headers()).containsKey("cookie");
        assertThat(redacted.request().query()).isNull();
        assertThat(redacted.request().body()).contains("\"count\":\"***\"");
        assertThat(redacted.request().body()).contains("\"safe\":\"***\"");
    }

    @Test
    void headerKeepRuleRetainsHeaderWhenHeaderFallbackIsRemove() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.REMOVE);
        redaction.setQueryParamDefaultAction(RedactionAction.MASK);
        redaction.setJsonFieldDefaultAction(RedactionAction.MASK);
        redaction.setRules(List.of(
                rule(RedactionTargetType.HEADER, "content-type", RedactionMatcherType.EXACT, RedactionAction.KEEP, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "page=1"));

        assertThat(redacted.request().headers()).containsKey("content-type");
        assertThat(redacted.request().headers().get("content-type")).containsExactly("application/json");
    }

    @Test
    void defaultsMaskHeadersQueryAndAllTopLevelJsonFields() {
        RedactionConfig redaction = RedactionConfig.defaults();

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope(
                """
                {"password":"123","nested":{"token":"abc","safe":"ok"},"items":[{"secret":"x"}]}
                """,
                "token=abc&page=1"
        ));

        assertThat(redacted.request().headers().get("authorization")).containsExactly("***");
        assertThat(redacted.request().query()).isEqualTo("token=***&page=***");
        assertThat(redacted.request().body()).contains("\"password\":\"***\"");
        assertThat(redacted.request().body()).contains("\"nested\":\"***\"");
        assertThat(redacted.request().body()).contains("\"items\":\"***\"");
    }

    @Test
    void rulesOverrideDefaultsAndFirstMatchWins() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.MASK);
        redaction.setQueryParamDefaultAction(RedactionAction.MASK);
        redaction.setJsonFieldDefaultAction(RedactionAction.KEEP);

        RedactionRule keepByMaskOverride = rule(
                RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.MASK, "safe"
        );
        RedactionRule secondRuleShouldNotApply = rule(
                RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.REMOVE, null
        );
        RedactionRule removeJsonSecret = rule(
                RedactionTargetType.JSON_FIELD, "secret", RedactionMatcherType.EXACT, RedactionAction.REMOVE, null
        );

        redaction.setRules(List.of(keepByMaskOverride, secondRuleShouldNotApply, removeJsonSecret));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"secret\":\"s1\",\"other\":1}", "token=abc"));

        assertThat(redacted.request().query()).isEqualTo("token=safe");
        assertThat(redacted.request().body()).doesNotContain("\"secret\"");
        assertThat(redacted.request().body()).contains("\"other\":1");
    }

    @Test
    void queryMaskPreservesFlagStyleParameterSyntax() {
        RedactionConfig redaction = defaultsWithActions(RedactionAction.MASK, RedactionAction.KEEP, RedactionAction.MASK);
        redaction.setRules(List.of(
                rule(RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "token&page=1"));

        assertThat(redacted.request().query()).isEqualTo("token&page=1");
    }

    @Test
    void queryMaskPreservesTrailingEmptySegments() {
        RedactionConfig redaction = RedactionConfig.defaults();
        redaction.setRules(List.of(
                rule(RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "token=abc&"));

        assertThat(redacted.request().query()).isEqualTo("token=***&");
    }

    @Test
    void queryMaskPreservesEmptyValueShape() {
        RedactionConfig redaction = defaultsWithActions(RedactionAction.MASK, RedactionAction.KEEP, RedactionAction.MASK);
        redaction.setRules(List.of(
                rule(RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "token=&page=1"));

        assertThat(redacted.request().query()).isEqualTo("token=***&page=1");
    }

    @Test
    void removingAllQueryParametersReturnsNullQuery() {
        RedactionConfig redaction = RedactionConfig.defaults();
        redaction.setRules(List.of(
                rule(RedactionTargetType.QUERY_PARAM, "token", RedactionMatcherType.EXACT, RedactionAction.REMOVE, null),
                rule(RedactionTargetType.QUERY_PARAM, "page", RedactionMatcherType.EXACT, RedactionAction.REMOVE, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "token=abc&page=1"));

        assertThat(redacted.request().query()).isNull();
    }

    @Test
    void jsonFieldMatchingIsCaseSensitive() {
        RedactionConfig redaction = defaultsWithActions(RedactionAction.MASK, RedactionAction.MASK, RedactionAction.KEEP);
        redaction.setRules(List.of(
                rule(RedactionTargetType.JSON_FIELD, "password", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"Password\":\"X\",\"password\":\"Y\"}", "page=1"));

        assertThat(redacted.request().body()).contains("\"Password\":\"X\"");
        assertThat(redacted.request().body()).contains("\"password\":\"***\"");
    }

    @Test
    void headerAndQueryMatchingAreCaseInsensitive() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.MASK);
        redaction.setQueryParamDefaultAction(RedactionAction.MASK);
        redaction.setJsonFieldDefaultAction(RedactionAction.MASK);
        redaction.setRules(List.of(
                rule(RedactionTargetType.HEADER, "Authorization", RedactionMatcherType.EXACT, RedactionAction.MASK, null),
                rule(RedactionTargetType.QUERY_PARAM, "Token", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"ok\":1}", "token=abc"));

        assertThat(redacted.request().headers().get("authorization")).containsExactly("***");
        assertThat(redacted.request().query()).isEqualTo("token=***");
    }

    @Test
    void globAndRegexRulesAreApplied() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.KEEP);
        redaction.setQueryParamDefaultAction(RedactionAction.KEEP);
        redaction.setJsonFieldDefaultAction(RedactionAction.KEEP);
        redaction.setRules(List.of(
                rule(RedactionTargetType.HEADER, "x-*-token", RedactionMatcherType.GLOB, RedactionAction.MASK, null),
                rule(RedactionTargetType.QUERY_PARAM, "pa.*", RedactionMatcherType.REGEX, RedactionAction.MASK, "x")
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelopeWithHeaders("{\"ok\":1}", "page=1&token=2",
                Map.of("x-api-token", List.of("secret"), "content-type", List.of("application/json"))
        ));

        assertThat(redacted.request().headers().get("x-api-token")).containsExactly("***");
        assertThat(redacted.request().query()).isEqualTo("page=x&token=2");
    }

    @Test
    void headerMaskKeepsEmptyValueListShape() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("***");
        redaction.setHeaderDefaultAction(RedactionAction.MASK);
        redaction.setQueryParamDefaultAction(RedactionAction.MASK);
        redaction.setJsonFieldDefaultAction(RedactionAction.MASK);
        redaction.setRules(List.of(
                rule(RedactionTargetType.HEADER, "x-empty", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelopeWithHeaders(
                "{\"ok\":1}",
                "page=1",
                Map.of("x-empty", List.of())
        ));

        assertThat(redacted.request().headers().get("x-empty")).isEmpty();
    }

    @Test
    void maskedJsonValuesBecomeStringsByContract() {
        RedactionConfig redaction = RedactionConfig.defaults();
        redaction.setRules(List.of(
                rule(RedactionTargetType.JSON_FIELD, "count", RedactionMatcherType.EXACT, RedactionAction.MASK, null)
        ));

        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        CaptureEnvelope redacted = redactor.redact(sampleEnvelope("{\"count\":42}", "page=1"));

        assertThat(redacted.request().body()).contains("\"count\":\"***\"");
    }

    @Test
    void malformedJsonBodyUsesJsonFallbackDecision() {
        RedactionConfig redaction = RedactionConfig.defaults();
        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);
        String malformedBody = "{\"password\":\"x\"";

        CaptureEnvelope redacted = redactor.redact(sampleEnvelope(malformedBody, "token=abc"));

        assertThat(redacted.request().body()).isEqualTo("***");
    }

    @Test
    void nonJsonContentTypeUsesJsonFallbackDecision() {
        RedactionConfig redaction = RedactionConfig.defaults();
        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);

        CaptureEnvelope source = sampleEnvelope("password=plain", "token=abc");
        CaptureEnvelope.RequestCapture request = new CaptureEnvelope.RequestCapture(
                source.request().method(),
                source.request().path(),
                source.request().query(),
                source.request().headers(),
                source.request().body(),
                "text/plain",
                source.request().truncation()
        );
        CaptureEnvelope envelope = new CaptureEnvelope(
                source.schemaVersion(),
                source.recordedAt(),
                source.transport(),
                request,
                source.response()
        );

        CaptureEnvelope redacted = redactor.redact(envelope);
        assertThat(redacted.request().body()).isEqualTo("***");
    }

    @Test
    void nonJsonContentTypeCanBeKeptByJsonFallbackConfiguration() {
        RedactionConfig redaction = defaultsWithActions(RedactionAction.MASK, RedactionAction.MASK, RedactionAction.KEEP);
        PolicyBasedCaptureRedactor redactor = new PolicyBasedCaptureRedactor(objectMapper, redaction);

        CaptureEnvelope source = sampleEnvelope("password=plain", "token=abc");
        CaptureEnvelope.RequestCapture request = new CaptureEnvelope.RequestCapture(
                source.request().method(),
                source.request().path(),
                source.request().query(),
                source.request().headers(),
                source.request().body(),
                "text/plain",
                source.request().truncation()
        );
        CaptureEnvelope envelope = new CaptureEnvelope(
                source.schemaVersion(),
                source.recordedAt(),
                source.transport(),
                request,
                source.response()
        );

        CaptureEnvelope redacted = redactor.redact(envelope);
        assertThat(redacted.request().body()).isEqualTo("password=plain");
    }

    private static RedactionRule rule(
            RedactionTargetType type,
            String target,
            RedactionMatcherType matcherType,
            RedactionAction action,
            String replacement
    ) {
        RedactionRule rule = new RedactionRule();
        rule.setType(type);
        rule.setTarget(target);
        rule.setMatcher(matcherType);
        rule.setAction(action);
        rule.setReplacement(replacement);
        return rule;
    }

    private static RedactionConfig defaultsWithActions(
            RedactionAction headerDefaultAction,
            RedactionAction queryParamDefaultAction,
            RedactionAction jsonFieldDefaultAction
    ) {
        RedactionConfig config = RedactionConfig.defaults();
        config.setHeaderDefaultAction(headerDefaultAction);
        config.setQueryParamDefaultAction(queryParamDefaultAction);
        config.setJsonFieldDefaultAction(jsonFieldDefaultAction);
        return config;
    }

    private static CaptureEnvelope sampleEnvelope(String requestBody, String query) {
        return sampleEnvelopeWithHeaders(
                requestBody,
                query,
                Map.of(
                        "authorization", List.of("Bearer token"),
                        "content-type", List.of("application/json"),
                        "x-secret", List.of("to-remove"),
                        "cookie", List.of("session=abc")
                )
        );
    }

    private static CaptureEnvelope sampleEnvelopeWithHeaders(
            String requestBody,
            String query,
            Map<String, List<String>> headers
    ) {
        return new CaptureEnvelope(
                "0.1.0",
                "2026-03-17T12:00:00Z",
                "HTTP",
                new CaptureEnvelope.RequestCapture(
                        "POST",
                        "/api/orders",
                        query,
                        new LinkedHashMap<>(headers),
                        requestBody,
                        "application/json",
                        new CaptureEnvelope.Truncation(false, requestBody.length(), requestBody.length())
                ),
                new CaptureEnvelope.ResponseCapture(
                        200,
                        Map.of(
                                "content-type", List.of("application/json"),
                                "set-cookie", List.of("server=1")
                        ),
                        "{\"token\":\"response-secret\"}",
                        "application/json",
                        10,
                        new CaptureEnvelope.Truncation(false, 26, 26)
                )
        );
    }
}
