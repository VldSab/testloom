package dev.testloom.spring.autoconfigure;

import dev.testloom.core.capture.application.port.CaptureFailureHandler;
import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.application.port.CaptureWriter;
import dev.testloom.core.capture.application.service.SafeCaptureRecorder;
import dev.testloom.core.capture.infrastructure.file.JsonFileCaptureWriter;
import dev.testloom.core.config.application.port.TestloomConfigLoader;
import dev.testloom.core.config.domain.model.TestloomConfig;
import dev.testloom.spring.capture.LoggingCaptureFailureHandler;
import dev.testloom.spring.mvc.MvcCaptureEnvelopeFactory;
import dev.testloom.spring.mvc.MvcCaptureFilter;
import dev.testloom.spring.mvc.MvcCapturePathMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests Testloom starter auto-configuration wiring.
 */
class TestloomAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    TestloomAutoConfiguration.class,
                    TestloomMvcAutoConfiguration.class
            ));

    @Test
    void enabledRecorderRegistersCaptureBeans() {
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .run(context -> {
                    assertThat(context.getBeansOfType(CaptureWriter.class)).hasSize(1);
                    assertThat(context.getBeansOfType(CaptureRecorder.class)).hasSize(1);
                    assertThat(context.getBeansOfType(MvcCaptureEnvelopeFactory.class)).hasSize(1);
                    assertThat(context.getBeansOfType(MvcCaptureFilter.class)).hasSize(1);
                });
    }

    @Test
    void enabledRecorderRegistersExpectedDefaultImplementations() {
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .run(context -> {
                    assertThat(context.getBean(CaptureWriter.class)).isInstanceOf(JsonFileCaptureWriter.class);
                    assertThat(context.getBean(CaptureFailureHandler.class)).isInstanceOf(LoggingCaptureFailureHandler.class);
                    assertThat(context.getBean(CaptureRecorder.class)).isInstanceOf(SafeCaptureRecorder.class);
                });
    }

    @Test
    void configBeanIsAlwaysLoadedEvenWhenRecorderDisabled() {
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::disabledConfig)
                .run(context -> {
                    assertThat(context.getBeansOfType(CaptureWriter.class)).hasSize(1);
                    assertThat(context.getBeansOfType(CaptureRecorder.class)).hasSize(1);
                    assertThat(context.getBeansOfType(MvcCaptureFilter.class)).hasSize(1);
                });
    }

    @Test
    void customCaptureWriterOverridesDefaultWriterBean() {
        CaptureWriter customWriter = envelope -> { };
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .withBean(CaptureWriter.class, () -> customWriter)
                .run(context -> assertThat(context.getBean(CaptureWriter.class)).isSameInstanceAs(customWriter));
    }

    @Test
    void customFailureHandlerOverridesDefaultFailureHandlerBean() {
        CaptureFailureHandler customHandler = (envelope, exception) -> { };
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .withBean(CaptureFailureHandler.class, () -> customHandler)
                .run(context -> assertThat(context.getBean(CaptureFailureHandler.class)).isSameInstanceAs(customHandler));
    }

    @Test
    void customCaptureRecorderOverridesDefaultRecorderBean() {
        CaptureRecorder customRecorder = envelope -> { };
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .withBean(CaptureRecorder.class, () -> customRecorder)
                .run(context -> assertThat(context.getBean(CaptureRecorder.class)).isSameInstanceAs(customRecorder));
    }

    @Test
    void customPathMatcherOverridesDefaultMatcherBean() {
        MvcCapturePathMatcher matcher = (request, recorder) -> true;
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .withBean(MvcCapturePathMatcher.class, () -> matcher)
                .run(context -> assertThat(context.getBean(MvcCapturePathMatcher.class)).isSameInstanceAs(matcher));
    }

    @Test
    void filterRegistrationBeanHasExpectedNameOrderAndPatterns() {
        contextRunner
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .run(context -> {
                    FilterRegistrationBean<?> registrationBean = context.getBean(
                            "testloomMvcCaptureFilterRegistration",
                            FilterRegistrationBean.class
                    );

                    assertThat(registrationBean.getFilterName()).isEqualTo("testloomMvcCaptureFilter");
                    assertThat(registrationBean.getOrder()).isEqualTo(Integer.MAX_VALUE - 10);
                    assertThat(registrationBean.getUrlPatterns()).containsExactly("/*");
                });
    }

    @Test
    void nonWebContextRegistersOnlyCommonBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        TestloomAutoConfiguration.class
                ))
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .run(context -> {
                    assertThat(context.getBeansOfType(CaptureWriter.class)).hasSize(1);
                    assertThat(context.getBeansOfType(CaptureRecorder.class)).hasSize(1);
                    assertThat(context.getBeansOfType(CaptureFailureHandler.class)).hasSize(1);
                });
    }

    @Test
    void mvcAutoconfigurationDoesNotActivateInNonWebContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        TestloomAutoConfiguration.class,
                        TestloomMvcAutoConfiguration.class
                ))
                .withBean(TestloomConfig.class, TestloomAutoConfigurationTest::enabledConfig)
                .run(context -> assertThat(context.getBeansOfType(MvcCaptureFilter.class)).isEmpty());
    }

    @Test
    void testloomConfigBeanDelegatesToLoaderWithDefaultPath() {
        TestloomAutoConfiguration configuration = new TestloomAutoConfiguration();
        TestloomConfig expected = enabledConfig();
        AtomicReference<Path> seenPath = new AtomicReference<>();
        TestloomConfigLoader loader = path -> {
            seenPath.set(path);
            return expected;
        };

        TestloomConfig actual = configuration.testloomConfig(loader);

        assertThat(actual).isSameInstanceAs(expected);
        assertThat(seenPath.get()).isEqualTo(Path.of("testloom.yaml"));
    }

    private static TestloomConfig enabledConfig() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setEnabled(true);
        config.getRecorder().setIncludeBodies(true);
        config.getRecorder().setMaxBodySizeBytes(1024);
        return config;
    }

    private static TestloomConfig disabledConfig() {
        TestloomConfig config = TestloomConfig.defaults();
        config.getRecorder().setEnabled(false);
        return config;
    }

}
