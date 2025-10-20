package org.finos.fluxnova.config;

import org.finos.fluxnova.mapper.AuthorizationProperties;
import org.finos.fluxnova.mapper.GroupProperties;
import org.finos.fluxnova.mapper.UserProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@PropertySource(value="classpath:user-config.yaml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "fluxnova")
public class UserConfig {
    private UserProperties user;
    private List<GroupProperties> groups;
    private List<AuthorizationProperties> authorization;

    public UserProperties getUser() { return user; }
    public void setUser(UserProperties user) { this.user = user; }
    public List<GroupProperties> getGroups() { return groups; }
    public void setGroups(List<GroupProperties> groups) { this.groups = groups; }
    public List<AuthorizationProperties> getAuthorization() { return authorization; }
    public void setAuthorization(List<AuthorizationProperties> authorization) { this.authorization = authorization; }
}
