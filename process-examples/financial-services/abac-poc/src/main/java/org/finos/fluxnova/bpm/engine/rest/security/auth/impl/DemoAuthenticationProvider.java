package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.finos.fluxnova.bpm.engine.ProcessEngine;

import java.util.Map;

/**
 * Demo-only authentication provider that allows a small in-memory set of personas
 * without persisting users into the engine identity store.
 */
public class DemoAuthenticationProvider extends HttpBasicAuthenticationProvider {

    private static final Map<String, String> DEMO_USERS = Map.of(
            "demo", "demo",
            "wealth-us-user", "demo",
            "operations-eu-user", "demo",
            "apac-wealth-user", "demo");

    @Override
    protected boolean isAuthenticated(ProcessEngine processEngine, String userName, String password) {
        String expectedPassword = DEMO_USERS.get(userName);
        if (expectedPassword != null && expectedPassword.equals(password)) {
            return true;
        }

        return super.isAuthenticated(processEngine, userName, password);
    }
}
