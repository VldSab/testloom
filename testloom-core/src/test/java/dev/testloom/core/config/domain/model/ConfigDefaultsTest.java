package dev.testloom.core.config.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests default config values and default enum value sets.
 */
class ConfigDefaultsTest {
    @Test
    void recorderDefaultsAreStable() {
        RecorderConfig defaults = RecorderConfig.defaults();

        assertThat(defaults.isEnabled()).isFalse();
        assertThat(defaults.getMode()).isEqualTo(RecorderMode.LOCAL);
        assertThat(defaults.getOutputDir()).isEqualTo("./.testloom/captures");
        assertThat(defaults.isIncludeBodies()).isTrue();
        assertThat(defaults.getMaxBodySizeBytes()).isEqualTo(65536);
        assertThat(defaults.getIncludePaths()).containsExactly("/api/**").inOrder();
        assertThat(defaults.getExcludePaths()).containsExactly("/actuator/**").inOrder();
    }

    @Test
    void redactionDefaultsAreStable() {
        RedactionConfig defaults = RedactionConfig.defaults();

        assertThat(defaults.getMask()).isEqualTo("***");
        assertThat(defaults.getHeaders()).containsExactly("authorization", "cookie", "set-cookie").inOrder();
        assertThat(defaults.getJsonFields()).containsExactly("password", "token", "secret").inOrder();
        assertThat(defaults.getQueryParams()).containsExactly("token", "api_key").inOrder();
        assertThat(defaults.getRules()).isEmpty();
    }

    @Test
    void testloomDefaultsComposeNestedDefaults() {
        TestloomConfig defaults = TestloomConfig.defaults();

        assertThat(defaults.getRecorder()).isNotNull();
        assertThat(defaults.getRedaction()).isNotNull();
        assertThat(defaults.getRecorder().getMode()).isEqualTo(RecorderMode.LOCAL);
        assertThat(defaults.getRedaction().getMask()).isEqualTo("***");
    }

    @Test
    void defaultEnumListsAreImmutable() {
        List<String> headers = DefaultRedactionHeader.valuesList();
        List<String> fields = DefaultRedactionJsonField.valuesList();
        List<String> params = DefaultRedactionQueryParam.valuesList();

        assertThrows(UnsupportedOperationException.class, () -> headers.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> fields.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> params.add("x"));
    }

    @Test
    void enumValueMethodsReturnExpectedRawValues() {
        assertThat(DefaultRedactionHeader.AUTHORIZATION.value()).isEqualTo("authorization");
        assertThat(DefaultRedactionHeader.COOKIE.value()).isEqualTo("cookie");
        assertThat(DefaultRedactionHeader.SET_COOKIE.value()).isEqualTo("set-cookie");
        assertThat(DefaultRedactionJsonField.PASSWORD.value()).isEqualTo("password");
        assertThat(DefaultRedactionJsonField.TOKEN.value()).isEqualTo("token");
        assertThat(DefaultRedactionJsonField.SECRET.value()).isEqualTo("secret");
        assertThat(DefaultRedactionQueryParam.TOKEN.value()).isEqualTo("token");
        assertThat(DefaultRedactionQueryParam.API_KEY.value()).isEqualTo("api_key");
    }
}
