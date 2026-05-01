package co.summit58.feewaiver.sb.app;

import org.finos.fluxnova.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.finos.fluxnova.bpm.spring.boot.starter.event.PostDeployEvent;

import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

import java.sql.SQLException;

import java.util.logging.Logger;

/**
 * Runs the Spring Boot application.
 *
 * Since we're using software with no history reporting in Cockpit/Monitoring, we need
 *   a way to access our database. Thus, we'll create a TCP connection to H2 and create a web server
 *   that will allow us to access it via the H2 web console. In this case, navigate to this URL to access
 *   it: http://localhost:8090.
 *
 * @author Ryan Johnston, Managing Principal, Summit58 LLC
 */
@SpringBootApplication(scanBasePackages="co.summit58.feewaiver.sb.cfg")
@EnableProcessApplication("feewaiver-sb")
public class FeeWaiverSBProcessApplication {

    private static final Logger LOG = Logger.getLogger(FeeWaiverSBProcessApplication.class.getName());

    public static void main(String... args) {
        LOG.fine("Starting the H2 TCP server...");
        startH2Server();

        SpringApplication.run(FeeWaiverSBProcessApplication.class, args);
    }

    private static void startH2Server() {
        try {
            Server h2TcpServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists").start();

            if(h2TcpServer.isRunning(true)) {
                return;
            }
            else {
                throw new RuntimeException("Failed to start the TCP server for H2. Exiting.");
            }
        }
        catch(SQLException sqlEx) {
            throw new RuntimeException("Failed to start the TCP server for H2. Exiting.");
        }
    }

    public static void startWebServer() {
        try {
            Server h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8090").start();

            //The web server isn't critical to application startup. However, we want to notify the user whether
            //  it started successfully or not.
            if(h2WebServer.isRunning(true))
                LOG.fine("The H2 web server was started successfully.");
            else
                LOG.severe("The H2 web server was not started successfully.");
        }
        catch(SQLException sqlEx) {
            LOG.info("SQLException generated on startup of H2 web server (web server not required): " + sqlEx.toString());
        }
    }

    @EventListener
    public void onPostDeploy(PostDeployEvent event) {
    }

    @EventListener
    private void processPostDeploy(PostDeployEvent event) {
    }

}
