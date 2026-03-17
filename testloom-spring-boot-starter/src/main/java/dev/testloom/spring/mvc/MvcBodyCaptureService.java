package dev.testloom.spring.mvc;

import dev.testloom.core.capture.domain.model.CaptureEnvelope;

import java.nio.charset.Charset;

/**
 * Builds captured body payload and truncation metadata.
 *
 * <p>The service accepts both cached bytes and optional declared body length
 * (typically {@code Content-Length}). This allows it to preserve a more accurate
 * {@code originalSizeBytes} value when wrappers expose fewer bytes than were
 * originally sent over HTTP.
 */
final class MvcBodyCaptureService {
    /**
     * Captures body string and truncation metadata.
     *
     * @param bytes cached body bytes
     * @param charset charset used to decode body
     * @param declaredLength declared body length from transport metadata (for example
     *                       {@code Content-Length}), or {@code -1} when unknown
     * @param includeBodies whether body capture is enabled
     * @param maxBodyBytes max captured body size in bytes
     * @return captured body payload with truncation metadata
     */
    MvcCapturedBody capture(
            byte[] bytes,
            Charset charset,
            long declaredLength,
            boolean includeBodies,
            int maxBodyBytes
    ) {
        int cachedLength = bytes == null ? 0 : bytes.length;
        int originalLength = resolveOriginalLength(cachedLength, declaredLength);

        if (!includeBodies) {
            return new MvcCapturedBody(null, new CaptureEnvelope.Truncation(false, originalLength, 0));
        }
        if (maxBodyBytes <= 0) {
            throw new IllegalArgumentException("maxBodyBytes must be > 0 when includeBodies is enabled.");
        }
        if (originalLength == 0) {
            return new MvcCapturedBody(null, new CaptureEnvelope.Truncation(false, 0, 0));
        }

        int capturedLength = Math.min(cachedLength, maxBodyBytes);
        String body = capturedLength > 0 ? new String(bytes, 0, capturedLength, charset) : null;
        return new MvcCapturedBody(
                body,
                new CaptureEnvelope.Truncation(originalLength > capturedLength, originalLength, capturedLength)
        );
    }

    /**
     * Resolves best-effort original body size used for truncation metadata.
     *
     * <p>When declared length is available and larger than cached bytes, declared
     * length wins. Unknown declared length ({@code -1}) naturally falls back to
     * cached length through the same comparison path ({@code max(declaredLength, cachedLength)}).
     *
     * <p>{@code cachedLength} is always non-negative.
     */
    private int resolveOriginalLength(int cachedLength, long declaredLength) {
        long normalized = declaredLength > cachedLength ? declaredLength : cachedLength;
        return normalized > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) normalized;
    }

    /**
     * Captured body and its size metadata.
     *
     * @param body decoded body string, may be {@code null}
     * @param truncation truncation metadata
     */
    record MvcCapturedBody(String body, CaptureEnvelope.Truncation truncation) {
    }
}
