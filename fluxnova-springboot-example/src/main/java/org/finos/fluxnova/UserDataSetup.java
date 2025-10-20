package org.finos.fluxnova;

import org.finos.fluxnova.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserDataSetup implements  CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(UserDataSetup.class);
    private final UserService userService;

    @Value("${fluxnova.bpm.test-user-setup.enabled:false}")
    private boolean enableTestUserSetup;

    @Autowired
    public UserDataSetup(UserService userService) {
        this.userService = userService;
    }


    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Test user setup enabled: {}", enableTestUserSetup);
            if (enableTestUserSetup) {
                logger.info("Setting up default user group and authorizations.");
                userService.setupDefaultUserGroupAndAuthorizations();
                logger.info("Default user group and authorizations setup completed.");
            } else {
                logger.info("Test user setup is disabled. Skipping setup.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
