package co.summit58.feewaiver.sb.cfg;

import co.summit58.feewaiver.sb.app.FeeWaiverSBProcessApplication;
import org.finos.fluxnova.bpm.engine.ProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.finos.fluxnova.bpm.spring.boot.starter.configuration.FluxnovaAuthorizationConfiguration;
import org.finos.fluxnova.bpm.spring.boot.starter.configuration.impl.DefaultAuthorizationConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Default process engine configuration.
 *
 * @author Ryan Johnston, Managing Principal, Summit58 LLC
 */
@Configuration
@Profile("prod")
@ComponentScan({"co.summit58.feewaiver.sb.delegates","co.summit58.feewaiver.sb.listeners",
                "co.summit58.feewaiver.sb.services", "co.summit58.feewaiver.sb.controllers",
                "co.summit58.feewaiver.sb.ui.services", "co.summit58.feewaiver.sb.ui.controllers"})
@EnableJpaRepositories(basePackages = "co.summit58.feewaiver.sb.entities")
@EntityScan(basePackages = "co.summit58.feewaiver.sb.entities")
public class DefaultProcessEngineConfiguration {

    private final Logger LOG = Logger.getLogger(DefaultProcessEngineConfiguration.class.getName());

    @Bean
    @Primary
    @ConfigurationProperties(prefix="datasource.primary")
    public DataSource primaryDataSource() {
        //Start the H2 web server as well.
        FeeWaiverSBProcessApplication.startWebServer();

        return buildH2TcpDataSource();
    }

    private javax.sql.DataSource buildH2TcpDataSource() {
        LOG.fine("Building a simple TCP data source (H2)...");
        SimpleDriverDataSource springDataSource = new SimpleDriverDataSource();
        springDataSource.setDriverClass(org.h2.Driver.class);
        springDataSource.setUrl("jdbc:h2:tcp://localhost/./feewaiver-sb/fluxnova");
        springDataSource.setUsername("sa");
        springDataSource.setPassword("sa");
        return springDataSource;
    }

    /**
     * Allows for the overriding of the default authorization configuration settings for Spring Boot apps.
     *
     * Note that this also sets the default retry time cycle to "R3/PT30S".
     */
    @Bean
    public FluxnovaAuthorizationConfiguration fluxnovaAuthorizationConfiguration() {
        DefaultAuthorizationConfiguration authConfig = new DefaultAuthorizationConfiguration() {
            @Override
            public void preInit(final SpringProcessEngineConfiguration configuration) {
                super.preInit(configuration);
                configuration.setAuthorizationCheckRevokes(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_ALWAYS);
                configuration.setAuthorizationEnabled(true);
                configuration.setFailedJobRetryTimeCycle("R3/PT30S");
                configuration.setEnforceHistoryTimeToLive(false);
            }
        };
        return authConfig;
    }

}
