package dev.testloom.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.testloom.core.capture.application.port.CaptureFailureHandler;
import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.application.port.CaptureWriter;
import dev.testloom.core.capture.application.service.SafeCaptureRecorder;
import dev.testloom.core.capture.infrastructure.file.JsonFileCaptureWriter;
import dev.testloom.core.config.application.port.TestloomConfigLoader;
import dev.testloom.core.config.domain.model.TestloomConfig;
import dev.testloom.core.config.infrastructure.yaml.YamlTestloomConfigLoader;
import dev.testloom.core.redaction.application.port.CaptureRedactor;
import dev.testloom.core.redaction.application.service.PolicyBasedCaptureRedactor;
import dev.testloom.spring.capture.LoggingCaptureFailureHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Common autoconfiguration for Testloom recorder support.
 */
@AutoConfiguration
public class TestloomAutoConfiguration {
    private static final String DEFAULT_CONFIG_PATH = "testloom.yaml";

    /**
     * Creates default YAML config loader.
     *
     * @return config loader
     */
    @Bean
    @ConditionalOnMissingBean
    public TestloomConfigLoader testloomConfigLoader() {
        return new YamlTestloomConfigLoader();
    }

    /**
     * Loads Testloom config from the default config file.
     *
     * @param loader config loader
     * @return loaded config
     */
    @Bean
    @ConditionalOnMissingBean
    public TestloomConfig testloomConfig(TestloomConfigLoader loader) {
        return loader.load(Path.of(DEFAULT_CONFIG_PATH));
    }

    /**
     * Creates the default writer for capture persistence.
     *
     * @param objectMapper Jackson mapper provided by Spring Boot
     * @param config       loaded testloom config
     * @return writer that writes envelopes as JSON files
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptureWriter captureWriter(ObjectMapper objectMapper, TestloomConfig config) {
        return new JsonFileCaptureWriter(objectMapper, config.getRecorder().getOutputDir());
    }

    /**
     * Creates capture redactor used before persistence.
     *
     * @param objectMapper jackson mapper used for JSON redaction
     * @param config loaded testloom config
     * @return capture redactor
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptureRedactor captureRedactor(ObjectMapper objectMapper, TestloomConfig config) {
        return new PolicyBasedCaptureRedactor(objectMapper, config.getRedaction());
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
     * @param captureWriter  destination writer for successful capture writes
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
