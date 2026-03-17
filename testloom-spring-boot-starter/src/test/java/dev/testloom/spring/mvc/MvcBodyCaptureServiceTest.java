package dev.testloom.spring.mvc;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests body capture and truncation metadata rules.
 */
class MvcBodyCaptureServiceTest {
    private final MvcBodyCaptureService service = new MvcBodyCaptureService();

    @Test
    void includeBodiesFalseReturnsNullBodyAndMetadata() {
        byte[] bytes = "123456".getBytes(StandardCharsets.UTF_8);

        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                bytes,
                StandardCharsets.UTF_8,
                6,
                false,
                4
        );

        assertThat(captured.body()).isNull();
        assertThat(captured.truncation()).isEqualTo(new CaptureEnvelope.Truncation(false, 6, 0));
    }

    @Test
    void emptyBodyReturnsZeroMetadata() {
        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                new byte[0],
                StandardCharsets.UTF_8,
                0,
                true,
                128
        );

        assertThat(captured.body()).isNull();
        assertThat(captured.truncation()).isEqualTo(new CaptureEnvelope.Truncation(false, 0, 0));
    }

    @Test
    void bodyIsTruncatedWhenMaxLimitIsLowerThanBodySize() {
        byte[] bytes = "abcdef".getBytes(StandardCharsets.UTF_8);

        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                bytes,
                StandardCharsets.UTF_8,
                6,
                true,
                4
        );

        assertThat(captured.body()).isEqualTo("abcd");
        assertThat(captured.truncation()).isEqualTo(new CaptureEnvelope.Truncation(true, 6, 4));
    }

    @Test
    void usesDeclaredLengthWhenItIsGreaterThanCachedLength() {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);

        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                bytes,
                StandardCharsets.UTF_8,
                6,
                true,
                8
        );

        assertThat(captured.body()).isEqualTo("abc");
        assertThat(captured.truncation()).isEqualTo(new CaptureEnvelope.Truncation(true, 6, 3));
    }

    @Test
    void includeBodiesTrueRejectsNonPositiveLimit() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.capture(
                        "abc".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8,
                        3,
                        true,
                        0
                )
        );

        assertThat(error).hasMessageThat().contains("maxBodyBytes must be > 0");
    }

    @Test
    void includeBodiesTrueWithNullBytesAndDeclaredLengthCapturesNullBody() {
        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                null,
                StandardCharsets.UTF_8,
                10,
                true,
                4
        );

        assertThat(captured.body()).isNull();
        assertThat(captured.truncation()).isEqualTo(new CaptureEnvelope.Truncation(true, 10, 0));
    }

    @Test
    void originalLengthIsCappedAtIntegerMaxValue() {
        MvcBodyCaptureService.MvcCapturedBody captured = service.capture(
                "a".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8,
                Long.MAX_VALUE,
                true,
                16
        );

        assertThat(captured.truncation().originalSizeBytes()).isEqualTo(Integer.MAX_VALUE);
        assertThat(captured.truncation().capturedSizeBytes()).isEqualTo(1);
    }
}
