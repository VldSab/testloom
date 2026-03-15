package dev.testloom.spring.autoconfigure;

import dev.testloom.spring.capture.CaptureRecorder;
import dev.testloom.spring.mvc.AntPatternMvcCapturePathMatcher;
import dev.testloom.spring.mvc.MvcCaptureFilter;
import dev.testloom.spring.mvc.MvcCapturePathMatcher;
import dev.testloom.spring.properties.TestloomProperties;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet MVC autoconfiguration for Testloom capture.
 */
@AutoConfiguration(after = TestloomAutoConfiguration.class)
@EnableConfigurationProperties(TestloomProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Filter.class, OncePerRequestFilter.class})
@ConditionalOnProperty(prefix = "testloom.recorder", name = "enabled", havingValue = "true")
public class TestloomMvcAutoConfiguration {
    private static final String FILTER_REGISTRATION_BEAN_NAME = "testloomMvcCaptureFilterRegistration";
    private static final String SERVLET_FILTER_NAME = "testloomMvcCaptureFilter";

    /**
     * Creates the default path matcher for MVC capture include/exclude patterns.
     *
     * @return ant-based MVC path matcher
     */
    @Bean
    @ConditionalOnMissingBean
    public MvcCapturePathMatcher mvcCapturePathMatcher() {
        return new AntPatternMvcCapturePathMatcher();
    }

    /**
     * Creates the servlet capture filter.
     *
     * @param captureRecorder safe recorder facade
     * @param properties runtime properties
     * @param pathMatcher include/exclude path matcher
     * @return MVC capture filter
     */
    @Bean
    @ConditionalOnMissingBean
    public MvcCaptureFilter mvcCaptureFilter(
            CaptureRecorder captureRecorder,
            TestloomProperties properties,
            MvcCapturePathMatcher pathMatcher
    ) {
        return new MvcCaptureFilter(captureRecorder, properties, pathMatcher);
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
