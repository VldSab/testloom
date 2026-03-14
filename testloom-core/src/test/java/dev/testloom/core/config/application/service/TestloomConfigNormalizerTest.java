package dev.testloom.core.config.application.service;

import dev.testloom.core.config.domain.model.RecorderConfig;
import dev.testloom.core.config.domain.model.RecorderMode;
import dev.testloom.core.config.domain.model.RedactionAction;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.config.domain.model.RedactionMatcherType;
import dev.testloom.core.config.domain.model.RedactionRule;
import dev.testloom.core.config.domain.model.RedactionTargetType;
import dev.testloom.core.config.domain.model.TestloomConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests normalization behavior for loaded config models.
 */
class TestloomConfigNormalizerTest {
    @Test
    void normalizeFailsWhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> TestloomConfigNormalizer.normalize(null));
    }

    @Test
    void normalizePreservesMissingSectionsForLinting() {
        TestloomConfig source = new TestloomConfig();

        TestloomConfig normalized = TestloomConfigNormalizer.normalize(source);

        assertThat(normalized.getRecorder()).isNull();
        assertThat(normalized.getRedaction()).isNull();
    }

    @Test
    void normalizeTrimsAndDeduplicatesRecorderFields() {
        RecorderConfig recorder = new RecorderConfig();
        recorder.setEnabled(true);
        recorder.setMode(RecorderMode.DEV);
        recorder.setOutputDir("   ");
        recorder.setIncludeBodies(false);
        recorder.setMaxBodySizeKb(256);
        recorder.setIncludePaths(new ArrayList<>(List.of("/api/**", " /api/** ", " /orders/** ")));
        recorder.setExcludePaths(new ArrayList<>(List.of(" /actuator/** ", "/actuator/**", "/health/**")));

        TestloomConfig source = new TestloomConfig();
        source.setRecorder(recorder);
        source.setRedaction(new RedactionConfig());

        TestloomConfig normalized = TestloomConfigNormalizer.normalize(source);

        assertThat(normalized.getRecorder().getMode()).isEqualTo(RecorderMode.DEV);
        assertThat(normalized.getRecorder().getOutputDir()).isNull();
        assertThat(normalized.getRecorder().getIncludePaths()).containsExactly("/api/**", "/orders/**").inOrder();
        assertThat(normalized.getRecorder().getExcludePaths()).containsExactly("/actuator/**", "/health/**").inOrder();
        assertThrows(UnsupportedOperationException.class, () -> normalized.getRecorder().getIncludePaths().add("/x"));
    }

    @Test
    void normalizeRedactionRulesAndCollections() {
        RedactionRule headerRule = new RedactionRule();
        headerRule.setType(RedactionTargetType.HEADER);
        headerRule.setTarget(" Authorization ");
        headerRule.setMatcher(RedactionMatcherType.EXACT);
        headerRule.setAction(RedactionAction.MASK);
        headerRule.setReplacement(" replacement ");

        RedactionRule queryRule = new RedactionRule();
        queryRule.setType(RedactionTargetType.QUERY_PARAM);
        queryRule.setTarget(" Token ");
        queryRule.setMatcher(RedactionMatcherType.EXACT);
        queryRule.setAction(RedactionAction.REMOVE);

        RedactionRule jsonRule = new RedactionRule();
        jsonRule.setType(RedactionTargetType.JSON_FIELD);
        jsonRule.setTarget(" Password ");
        jsonRule.setMatcher(RedactionMatcherType.EXACT);
        jsonRule.setAction(RedactionAction.MASK);

        RedactionConfig redaction = new RedactionConfig();
        redaction.setMask("  ###  ");
        redaction.setHeaders(new ArrayList<>(List.of(" Authorization ", "authorization", "X-API-Key")));
        redaction.setJsonFields(new ArrayList<>(List.of("password", " password ", " secret ")));
        redaction.setQueryParams(new ArrayList<>(List.of("Token", " token ", "api_key")));
        redaction.setRules(new ArrayList<>(Arrays.asList(headerRule, null, queryRule, jsonRule)));

        TestloomConfig source = new TestloomConfig();
        source.setRecorder(new RecorderConfig());
        source.setRedaction(redaction);

        TestloomConfig normalized = TestloomConfigNormalizer.normalize(source);

        assertThat(normalized.getRedaction().getMask()).isEqualTo("###");
        assertThat(normalized.getRedaction().getHeaders()).containsExactly("authorization", "x-api-key").inOrder();
        assertThat(normalized.getRedaction().getJsonFields()).containsExactly("password", "secret").inOrder();
        assertThat(normalized.getRedaction().getQueryParams()).containsExactly("token", "api_key").inOrder();
        assertThat(normalized.getRedaction().getRules()).hasSize(4);
        assertThat(normalized.getRedaction().getRules().get(0).getTarget()).isEqualTo("authorization");
        assertThat(normalized.getRedaction().getRules().get(1)).isNull();
        assertThat(normalized.getRedaction().getRules().get(2).getTarget()).isEqualTo("token");
        assertThat(normalized.getRedaction().getRules().get(3).getTarget()).isEqualTo("Password");
        assertThat(normalized.getRedaction().getRules().get(0).getReplacement()).isEqualTo("replacement");
        assertThrows(UnsupportedOperationException.class, () -> normalized.getRedaction().getRules().add(new RedactionRule()));
    }

    @Test
    void normalizeDropsNullAndBlankStringEntries() {
        RedactionConfig redaction = new RedactionConfig();
        redaction.setHeaders(new ArrayList<>(Arrays.asList("  ", "Authorization", null)));
        redaction.setJsonFields(new ArrayList<>(List.of("", "password", "   ")));
        redaction.setQueryParams(new ArrayList<>(Arrays.asList(null, " token ")));

        TestloomConfig source = new TestloomConfig();
        source.setRecorder(new RecorderConfig());
        source.setRedaction(redaction);

        TestloomConfig normalized = TestloomConfigNormalizer.normalize(source);

        assertThat(normalized.getRedaction().getHeaders()).containsExactly("authorization");
        assertThat(normalized.getRedaction().getJsonFields()).containsExactly("password");
        assertThat(normalized.getRedaction().getQueryParams()).containsExactly("token");
        assertThat(normalized.getRedaction().getRules()).isEmpty();
    }
}
