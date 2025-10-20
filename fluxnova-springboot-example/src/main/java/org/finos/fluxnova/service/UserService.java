package org.finos.fluxnova.service;

import org.finos.fluxnova.config.UserConfig;
import org.finos.fluxnova.mapper.AuthorizationProperties;
import org.finos.fluxnova.mapper.GroupProperties;
import org.finos.fluxnova.mapper.UserProperties;
import org.finos.fluxnova.model.Authorization;
import org.finos.fluxnova.model.Group;
import org.finos.fluxnova.model.User;
import org.finos.fluxnova.util.ResourcePermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class UserService {
    private final UserConfig userConfig;
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "http://localhost:8080/engine-rest";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${fluxnova.bpm.admin-user.id}")
    private String adminUserId;

    @Value("${fluxnova.bpm.admin-user.password}")
    private String adminUserPassword;

    @Autowired
    public UserService(UserConfig userConfig, RestTemplate restTemplate) {
        this.userConfig = userConfig;
        this.restTemplate = restTemplate;
    }

    public void setupDefaultUserGroupAndAuthorizations() {
        UserProperties userProps = userConfig.getUser();
        List<GroupProperties> groups = userConfig.getGroups();
        List<AuthorizationProperties> authorizations = userConfig.getAuthorization();
        // Read and display user configuration
        if (userProps != null) {
            // add log statement for user creation
            logger.info("Creating user with id: {}", userProps.getId());
            User.Profile  profile = new User.Profile();
            profile.setId(userProps.getId());
            profile.setFirstName(userProps.getFirstName());
            profile.setLastName(userProps.getLastName());
            profile.setEmail(userProps.getEmail());
            User.Credentials credentials = new User.Credentials();
            credentials.setPassword(userProps.getPassword());
            User user = new User();
            user.setProfile(profile);
            user.setCredentials(credentials);
            // Make REST API call to create user
            createUser(user);
        } else {
            logger.info("No user configuration found.");
        }

        // Read and display group configuration
        if (groups != null && !groups.isEmpty()) {
            for (GroupProperties group : groups) {
                logger.info("Creating group with id: {}", group.getId());
                Group grp = new Group();
                grp.setId(group.getId());
                grp.setName(group.getName());
                grp.setType(group.getType());
                createGroup(grp);
            }
        } else {
            logger.info("No group configuration found.");
        }

        // Add user to groups
        if (userProps != null && groups != null && !groups.isEmpty()) {
            for (GroupProperties group : groups) {
                logger.info("Adding User:{} with group  id: {}", userProps.getId(), group.getId());
                addUserToGroup( group.getId(), userProps.getId());

                // Read and display authorization configuration
                if (authorizations != null && !authorizations.isEmpty()) {
                    for (AuthorizationProperties auth : authorizations) {
                        ResourcePermissionMapper[] resourceTypes = ResourcePermissionMapper.values();
                        for (ResourcePermissionMapper resourceType : resourceTypes) {
                            Authorization authz = new Authorization();
                            authz.setResourceId(auth.getResourceId());
                            authz.setResourceType(resourceType.getResourceType());
                            authz.setPermissions(resourceType.getPermissions());
                            authz.setType(auth.getType());
                            authz.setGroupId(group.getId());
                            createAuthorization(authz);
                        }
                    }
                } else {
                    logger.info("No authorization configuration found.");
                }
            }
        }
    }

    public void createUser(User user) {
        String url = BASE_URL+"/user/create";
        HttpHeaders headers = getHeaders();
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("User creation response: {} ", response.getStatusCode());
        } catch (Exception e) {
            logger.error("User creation failed for UserId:{}, error: {}",user.getProfile().getId(), e.getMessage(), e);
        }
    }

    public void createGroup(Group group) {
        String url = BASE_URL+"/group/create";
        HttpHeaders headers = getHeaders();
        HttpEntity<Group> request = new HttpEntity<>(group, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("Group creation response: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Group creation failed groupId:{}, error: {}", group.getId(), e.getMessage(), e);
        }
    }

    public void addUserToGroup(String groupId, String userId ) {
        String url = BASE_URL+"/group/" + groupId+"/members/" + userId;
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.PUT,
                    request,
                    String.class
            );
            logger.info("Group update response: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Group update failed for GroupId:{}, error: {}",groupId, e.getMessage(), e);
        }
    }

    public void createAuthorization(Authorization authorization) {
        String url = BASE_URL+"/authorization/create";
        HttpHeaders headers = getHeaders();
        HttpEntity<Authorization> request = new HttpEntity<>(authorization, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("Authorization creation response: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("Authorization creation failed: {}", e.getMessage(), e);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addBasicAuthHeader(headers);
        return  headers;
    }

    private void addBasicAuthHeader(HttpHeaders headers) {
        String auth = adminUserId+":"+adminUserPassword;
        byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
    }

}
