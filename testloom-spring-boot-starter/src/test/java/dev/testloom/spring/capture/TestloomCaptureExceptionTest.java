package dev.testloom.spring.capture;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests constructors generated for {@link TestloomCaptureException}.
 */
class TestloomCaptureExceptionTest {
    @Test
    void supportsMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("boom");

        TestloomCaptureException exception = new TestloomCaptureException("write failed", cause);

        assertThat(exception).hasMessageThat().isEqualTo("write failed");
        assertThat(exception.getCause()).isSameInstanceAs(cause);
    }
}
