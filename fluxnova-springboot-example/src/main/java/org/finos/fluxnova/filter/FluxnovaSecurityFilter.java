package org.finos.fluxnova.filter;

import jakarta.servlet.Filter;
import org.finos.fluxnova.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FluxnovaSecurityFilter {
    @Bean
    public FilterRegistrationBean<Filter> processEngineAuthenticationFilter()
    {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setName("fluxnova-auth");
        registration.setFilter(new ProcessEngineAuthenticationFilter());
        registration.addInitParameter("authentication-provider", "org.finos.fluxnova.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider");
        registration.addUrlPatterns("/engine-rest/*");
        return registration;
    }
}
