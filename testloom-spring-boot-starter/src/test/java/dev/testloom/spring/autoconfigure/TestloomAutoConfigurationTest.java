package dev.testloom.spring.autoconfigure;

import dev.testloom.core.capture.application.port.CaptureFailureHandler;
import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.capture.application.port.CaptureWriter;
import dev.testloom.core.capture.application.service.SafeCaptureRecorder;
import dev.testloom.core.capture.infrastructure.file.JsonFileCaptureWriter;
import dev.testloom.spring.capture.LoggingCaptureFailureHandler;
import dev.testloom.spring.mvc.MvcCaptureFilter;
import dev.testloom.spring.mvc.MvcCapturePathMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

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
                .withPropertyValues("testloom.recorder.enabled=true")
                .run(context -> {
                    assertThat(context.getBeansOfType(CaptureWriter.class)).hasSize(1);
                    assertThat(context.getBeansOfType(CaptureRecorder.class)).hasSize(1);
                    assertThat(context.getBeansOfType(MvcCaptureFilter.class)).hasSize(1);
                });
    }

    @Test
    void enabledRecorderRegistersExpectedDefaultImplementations() {
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
                .run(context -> {
                    assertThat(context.getBean(CaptureWriter.class)).isInstanceOf(JsonFileCaptureWriter.class);
                    assertThat(context.getBean(CaptureFailureHandler.class)).isInstanceOf(LoggingCaptureFailureHandler.class);
                    assertThat(context.getBean(CaptureRecorder.class)).isInstanceOf(SafeCaptureRecorder.class);
                });
    }

    @Test
    void disabledRecorderSkipsCaptureBeans() {
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=false")
                .run(context -> {
                    assertThat(context.getBeansOfType(CaptureWriter.class)).isEmpty();
                    assertThat(context.getBeansOfType(CaptureRecorder.class)).isEmpty();
                    assertThat(context.getBeansOfType(MvcCaptureFilter.class)).isEmpty();
                });
    }

    @Test
    void customCaptureWriterOverridesDefaultWriterBean() {
        CaptureWriter customWriter = envelope -> { };
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
                .withBean(CaptureWriter.class, () -> customWriter)
                .run(context -> assertThat(context.getBean(CaptureWriter.class)).isSameInstanceAs(customWriter));
    }

    @Test
    void customFailureHandlerOverridesDefaultFailureHandlerBean() {
        CaptureFailureHandler customHandler = (envelope, exception) -> { };
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
                .withBean(CaptureFailureHandler.class, () -> customHandler)
                .run(context -> assertThat(context.getBean(CaptureFailureHandler.class)).isSameInstanceAs(customHandler));
    }

    @Test
    void customCaptureRecorderOverridesDefaultRecorderBean() {
        CaptureRecorder customRecorder = envelope -> { };
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
                .withBean(CaptureRecorder.class, () -> customRecorder)
                .run(context -> assertThat(context.getBean(CaptureRecorder.class)).isSameInstanceAs(customRecorder));
    }

    @Test
    void customPathMatcherOverridesDefaultMatcherBean() {
        MvcCapturePathMatcher matcher = (request, recorder) -> true;
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
                .withBean(MvcCapturePathMatcher.class, () -> matcher)
                .run(context -> assertThat(context.getBean(MvcCapturePathMatcher.class)).isSameInstanceAs(matcher));
    }

    @Test
    void filterRegistrationBeanHasExpectedNameOrderAndPatterns() {
        contextRunner
                .withPropertyValues("testloom.recorder.enabled=true")
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
                .withPropertyValues("testloom.recorder.enabled=true")
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
                .withPropertyValues("testloom.recorder.enabled=true")
                .run(context -> assertThat(context.getBeansOfType(MvcCaptureFilter.class)).isEmpty());
    }
}
