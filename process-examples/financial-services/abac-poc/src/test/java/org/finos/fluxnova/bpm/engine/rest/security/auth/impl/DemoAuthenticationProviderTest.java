package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoAuthenticationProviderTest {

    private final DemoAuthenticationProvider provider = new DemoAuthenticationProvider();

    @Test
    void authenticatesInMemoryDemoUsersWithoutTouchingIdentityStore() {
        ProcessEngine processEngine = mock(ProcessEngine.class);

        assertThat(provider.isAuthenticated(processEngine, "wealth-us-user", "demo")).isTrue();
        verifyNoInteractions(processEngine);
    }

    @Test
    void fallsBackToEngineIdentityStoreForOtherUsers() {
        ProcessEngine processEngine = mock(ProcessEngine.class);
        IdentityService identityService = mock(IdentityService.class);

        when(processEngine.getIdentityService()).thenReturn(identityService);
        when(identityService.checkPassword("someone-else", "secret")).thenReturn(true);

        assertThat(provider.isAuthenticated(processEngine, "someone-else", "secret")).isTrue();
    }
}
