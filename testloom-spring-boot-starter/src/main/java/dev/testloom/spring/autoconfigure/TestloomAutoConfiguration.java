package dev.testloom.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.spring.capture.CaptureFailureHandler;
import dev.testloom.spring.capture.CaptureRecorder;
import dev.testloom.spring.capture.CaptureWriter;
import dev.testloom.spring.capture.JsonFileCaptureWriter;
import dev.testloom.spring.capture.LoggingCaptureFailureHandler;
import dev.testloom.spring.capture.SafeCaptureRecorder;
import dev.testloom.spring.properties.TestloomProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Common autoconfiguration for Testloom recorder support.
 *
 * <p>When {@code testloom.recorder.enabled=true}, this configuration registers
 * shared capture beans used by transport-specific integrations.
 */
@AutoConfiguration
@EnableConfigurationProperties(TestloomProperties.class)
@ConditionalOnProperty(prefix = "testloom.recorder", name = "enabled", havingValue = "true")
public class TestloomAutoConfiguration {
    /**
     * Creates the default writer for capture persistence.
     *
     * @param objectMapper Jackson mapper provided by Spring Boot
     * @param properties Testloom runtime properties
     * @return writer that writes envelopes as JSON files
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptureWriter captureWriter(ObjectMapper objectMapper, TestloomProperties properties) {
        return new JsonFileCaptureWriter(objectMapper, properties.getRecorder().getOutputDir());
    }

    /**
     * Creates the default failure handler used when capture recording fails.
     *
     * @return logging-based capture failure handler
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptureFailureHandler captureFailureHandler() {
        return new LoggingCaptureFailureHandler();
    }

    /**
     * Creates the safe capture recorder facade.
     *
     * <p>This recorder never throws capture sink failures to request processing.
     *
     * @param captureWriter destination writer for successful capture writes
     * @param failureHandler callback for capture failures
     * @return safe capture recorder
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptureRecorder captureRecorder(
            CaptureWriter captureWriter,
            CaptureFailureHandler failureHandler
    ) {
        return new SafeCaptureRecorder(captureWriter, failureHandler);
    }
}
