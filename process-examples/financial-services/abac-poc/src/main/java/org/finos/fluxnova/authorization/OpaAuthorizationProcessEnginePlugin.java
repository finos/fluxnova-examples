package org.finos.fluxnova.authorization;

import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandInterceptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpaAuthorizationProcessEnginePlugin extends AbstractProcessEnginePlugin {

    private final OpaCompleteTaskAuthorizationInterceptor authorizationInterceptor;

    public OpaAuthorizationProcessEnginePlugin(OpaCompleteTaskAuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setCustomPostCommandInterceptorsTxRequired(
                prepend(processEngineConfiguration.getCustomPostCommandInterceptorsTxRequired(), authorizationInterceptor));

        processEngineConfiguration.setCustomPostCommandInterceptorsTxRequiresNew(
                prepend(processEngineConfiguration.getCustomPostCommandInterceptorsTxRequiresNew(), authorizationInterceptor));
    }

    private List<CommandInterceptor> prepend(List<CommandInterceptor> existing, CommandInterceptor interceptor) {
        List<CommandInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        if (existing != null) {
            interceptors.addAll(existing);
        }
        return interceptors;
    }
}

