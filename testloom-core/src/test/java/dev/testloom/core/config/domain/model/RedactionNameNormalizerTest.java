package dev.testloom.core.config.domain.model;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class RedactionNameNormalizerTest {
    @Test
    void normalizeHandlesNullBlankAndTargetSpecificCaseRules() {
        assertThat(RedactionNameNormalizer.normalize(null, RedactionTargetType.HEADER)).isNull();
        assertThat(RedactionNameNormalizer.normalize("   ", RedactionTargetType.HEADER)).isNull();

        assertThat(RedactionNameNormalizer.normalize(" Authorization ", RedactionTargetType.HEADER))
                .isEqualTo("authorization");
        assertThat(RedactionNameNormalizer.normalize(" Token ", RedactionTargetType.QUERY_PARAM))
                .isEqualTo("token");
        assertThat(RedactionNameNormalizer.normalize(" Password ", RedactionTargetType.JSON_FIELD))
                .isEqualTo("Password");
    }
}
