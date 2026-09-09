FROM finos/fluxnova-bpm-platform:latest

# Download the Keycloak Identity Provider Plugin for Camunda Run
ADD https://repo1.maven.org/maven2/org/camunda/bpm/extension/camunda-platform-7-keycloak-run/7.24.0/camunda-platform-7-keycloak-run-7.24.0.jar /fluxnova/configuration/userlib/camunda-platform-7-keycloak-run-7.24.0.jar

# Copy our custom configuration
COPY default.yml /fluxnova/configuration/default.yml
