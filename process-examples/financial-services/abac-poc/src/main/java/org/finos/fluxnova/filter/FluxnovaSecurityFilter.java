package org.finos.fluxnova.filter;

import jakarta.servlet.Filter;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FluxnovaSecurityFilter {

    @Bean
    public FilterRegistrationBean<Filter> processEngineAuthenticationFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setName("fluxnova-auth");
        registration.setFilter(new ProcessEngineAuthenticationFilter());
        registration.addInitParameter(
                "authentication-provider",
                "org.finos.fluxnova.bpm.engine.rest.security.auth.impl.DemoAuthenticationProvider");
        registration.addUrlPatterns("/engine-rest/*");
        return registration;
    }

    @Bean
    public AbacDemoAuthenticationFilter abacDemoAuthenticationFilter(IdentityService identityService) {
        return new AbacDemoAuthenticationFilter(identityService);
    }

    @Bean
    public FilterRegistrationBean<Filter> abacDemoAuthenticationFilterRegistration(AbacDemoAuthenticationFilter abacDemoAuthenticationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setName("abac-demo-auth");
        registration.setFilter(abacDemoAuthenticationFilter);
        registration.addUrlPatterns("/abac/*");
        return registration;
    }
}
