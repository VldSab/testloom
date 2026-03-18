package dev.testloom.spring.autoconfigure;

import dev.testloom.core.capture.application.port.CaptureRecorder;
import dev.testloom.core.config.domain.model.TestloomConfig;
import dev.testloom.core.redaction.application.port.CaptureRedactor;
import dev.testloom.spring.mvc.AntPatternMvcCapturePathMatcher;
import dev.testloom.spring.mvc.MvcCaptureEnvelopeFactory;
import dev.testloom.spring.mvc.MvcCaptureFilter;
import dev.testloom.spring.mvc.MvcCapturePathMatcher;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Clock;

/**
 * Servlet MVC autoconfiguration for Testloom capture.
 */
@AutoConfiguration(after = TestloomAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Filter.class, OncePerRequestFilter.class})
public class TestloomMvcAutoConfiguration {
    private static final String FILTER_REGISTRATION_BEAN_NAME = "testloomMvcCaptureFilterRegistration";
    private static final String SERVLET_FILTER_NAME = "testloomMvcCaptureFilter";

    /**
     * Creates the default clock used for capture timestamps.
     *
     * @return UTC clock
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock testloomCaptureClock() {
        return Clock.systemUTC();
    }

    /**
     * Creates capture-envelope factory for MVC exchanges.
     *
     * @param config loaded Testloom config
     * @param clock  timestamp source
     * @return envelope factory
     */
    @Bean
    @ConditionalOnMissingBean
    public MvcCaptureEnvelopeFactory mvcCaptureEnvelopeFactory(TestloomConfig config, Clock clock) {
        return new MvcCaptureEnvelopeFactory(config, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public MvcCapturePathMatcher mvcCapturePathMatcher() {
        return new AntPatternMvcCapturePathMatcher();
    }

    /**
     * Creates the servlet capture filter.
     *
     * @param captureRecorder safe recorder facade
     * @param config          loaded Testloom config
     * @param pathMatcher     include/exclude path matcher
     * @return MVC capture filter
     */
    @Bean
    @ConditionalOnMissingBean
    public MvcCaptureFilter mvcCaptureFilter(CaptureRecorder captureRecorder,
                                             TestloomConfig config,
                                             MvcCapturePathMatcher pathMatcher,
                                             MvcCaptureEnvelopeFactory envelopeFactory,
                                             CaptureRedactor captureRedactor) {
        return new MvcCaptureFilter(captureRecorder, config, pathMatcher, envelopeFactory, captureRedactor);
    }

    /**
     * Registers the MVC filter for all servlet routes.
     *
     * <p>The pattern {@code "/*"} maps the filter to all servlet requests; path
     * selection is applied inside {@link MvcCaptureFilter}.
     *
     * @param filter MVC capture filter bean
     * @return filter registration bean
     */
    @Bean(FILTER_REGISTRATION_BEAN_NAME)
    @ConditionalOnMissingBean(name = FILTER_REGISTRATION_BEAN_NAME)
    public FilterRegistrationBean<MvcCaptureFilter> testloomMvcCaptureFilterRegistration(MvcCaptureFilter filter) {
        FilterRegistrationBean<MvcCaptureFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setName(SERVLET_FILTER_NAME);
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
