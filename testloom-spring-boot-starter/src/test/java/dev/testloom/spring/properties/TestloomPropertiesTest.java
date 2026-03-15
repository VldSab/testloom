package dev.testloom.spring.properties;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests runtime properties container behavior.
 */
class TestloomPropertiesTest {
    @Test
    void setRecorderNullRestoresDefaultsContainer() {
        TestloomProperties properties = new TestloomProperties();
        TestloomProperties.RecorderProperties original = properties.getRecorder();

        properties.setRecorder(null);

        assertThat(properties.getRecorder()).isNotNull();
        assertThat(properties.getRecorder()).isNotSameInstanceAs(original);
    }
}
