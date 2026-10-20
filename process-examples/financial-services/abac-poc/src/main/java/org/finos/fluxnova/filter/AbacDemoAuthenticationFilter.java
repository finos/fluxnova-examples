package org.finos.fluxnova.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class AbacDemoAuthenticationFilter extends OncePerRequestFilter {

    private static final Map<String, String> DEMO_USERS = Map.of(
            "demo", "demo",
            "wealth-us-user", "demo",
            "operations-eu-user", "demo",
            "apac-wealth-user", "demo");

    private final IdentityService identityService;

    public AbacDemoAuthenticationFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"abac-demo\"");
            return;
        }

        String decoded = new String(
                Base64.getDecoder().decode(authorization.substring("Basic ".length())),
                StandardCharsets.UTF_8);
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"abac-demo\"");
            return;
        }

        String userName = decoded.substring(0, separator);
        String password = decoded.substring(separator + 1);
        String expectedPassword = DEMO_USERS.get(userName);
        if (expectedPassword == null || !expectedPassword.equals(password)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"abac-demo\"");
            return;
        }

        identityService.setAuthentication(userName, List.of(), List.of());
        try {
            filterChain.doFilter(request, response);
        } finally {
            identityService.clearAuthentication();
        }
    }
}
