package dev.testloom.core.redaction.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.core.capture.domain.model.CaptureEnvelope;
import dev.testloom.core.config.domain.model.RedactionConfig;
import dev.testloom.core.redaction.application.port.CaptureRedactor;
import dev.testloom.core.redaction.application.service.part.HeaderCaptureRedactor;
import dev.testloom.core.redaction.application.service.part.JsonBodyCaptureRedactor;
import dev.testloom.core.redaction.application.service.part.QueryCaptureRedactor;
import dev.testloom.core.redaction.application.service.part.RequestDraft;
import dev.testloom.core.redaction.application.service.part.RequestPartRedactor;
import dev.testloom.core.redaction.application.service.part.ResponseDraft;
import dev.testloom.core.redaction.application.service.part.ResponsePartRedactor;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicy;
import dev.testloom.core.redaction.application.service.policy.RedactionPolicyCompiler;

import java.util.List;
import java.util.Objects;

/**
 * Policy-based capture redactor.
 *
 * <p>The class orchestrates request/response redaction and delegates actual
 * format-specific work to dedicated components:
 * <ul>
 *     <li>{@link HeaderCaptureRedactor} for headers</li>
 *     <li>{@link QueryCaptureRedactor} for query string</li>
 *     <li>{@link JsonBodyCaptureRedactor} for JSON body fields</li>
 * </ul>
 *
 * <p>Extensibility path: request/response processing is defined as ordered lists
 * of part redactors. New transport parts (for example cookies or form-data)
 * can be plugged in by adding another step without changing envelope orchestration.
 *
 * <p>Configuration correctness is expected to be enforced by config loader+linter.
 * Runtime body parsing remains fail-safe: invalid JSON body is preserved as-is.
 */
public final class PolicyBasedCaptureRedactor implements CaptureRedactor {
    private final List<RequestPartRedactor> requestPartRedactors;
    private final List<ResponsePartRedactor> responsePartRedactors;

    /**
     * Creates redactor from normalized redaction config.
     *
     * @param objectMapper jackson mapper used for JSON body processing
     * @param redactionConfig normalized redaction section
     */
    public PolicyBasedCaptureRedactor(ObjectMapper objectMapper, RedactionConfig redactionConfig) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        RedactionConfig safeConfig = Objects.requireNonNull(redactionConfig, "redactionConfig must not be null");
        RedactionPolicy redactionPolicy = RedactionPolicyCompiler.compile(safeConfig);
        HeaderCaptureRedactor headerRedactor = new HeaderCaptureRedactor(redactionPolicy);
        QueryCaptureRedactor queryRedactor = new QueryCaptureRedactor(redactionPolicy);
        JsonBodyCaptureRedactor jsonBodyRedactor = new JsonBodyCaptureRedactor(objectMapper, redactionPolicy);
        this.requestPartRedactors = List.of(
                draft -> draft.query(queryRedactor.redact(draft.query())),
                draft -> draft.headers(headerRedactor.redact(draft.headers())),
                draft -> draft.body(jsonBodyRedactor.redactIfJson(draft.body(), draft.contentType()))
        );
        this.responsePartRedactors = List.of(
                draft -> draft.headers(headerRedactor.redact(draft.headers())),
                draft -> draft.body(jsonBodyRedactor.redactIfJson(draft.body(), draft.contentType()))
        );
    }

    @Override
    public CaptureEnvelope redact(CaptureEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        return new CaptureEnvelope(
                envelope.schemaVersion(),
                envelope.recordedAt(),
                envelope.transport(),
                redactRequest(envelope.request()),
                redactResponse(envelope.response())
        );
    }

    private CaptureEnvelope.RequestCapture redactRequest(CaptureEnvelope.RequestCapture request) {
        if (request == null) {
            return null;
        }
        RequestDraft draft = new RequestDraft(request);
        for (RequestPartRedactor redactor : requestPartRedactors) {
            redactor.apply(draft);
        }
        return draft.toCapture();
    }

    private CaptureEnvelope.ResponseCapture redactResponse(CaptureEnvelope.ResponseCapture response) {
        if (response == null) {
            return null;
        }
        ResponseDraft draft = new ResponseDraft(response);
        for (ResponsePartRedactor redactor : responsePartRedactors) {
            redactor.apply(draft);
        }
        return draft.toCapture();
    }
}
