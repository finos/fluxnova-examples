## Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY dependency/mcp-fluxnova-extension-0.1.0.jar /tmp/mcp-fluxnova-extension-0.1.0.jar
RUN mvn install:install-file \
    -Dfile=/tmp/mcp-fluxnova-extension-0.1.0.jar \
    -DgroupId=org.finos.fluxnova.ai.mcp \
    -DartifactId=mcp-fluxnova-extension \
    -Dversion=0.1.0 \
    -Dpackaging=jar

COPY fluxnova-service/pom.xml ./fluxnova-service/pom.xml
RUN mvn -f fluxnova-service/pom.xml dependency:go-offline

COPY fluxnova-service ./fluxnova-service
RUN mvn -f fluxnova-service/pom.xml clean package -DskipTests

## Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

RUN addgroup -S fluxnova && adduser -S fluxnova -G fluxnova
RUN mkdir -p /data/h2 && chown -R fluxnova:fluxnova /data

USER fluxnova

COPY --from=builder /workspace/fluxnova-service/target/*.jar app.jar

EXPOSE 8084 5005

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

VOLUME ["/data/h2"]

ENTRYPOINT ["java","-jar","app.jar"]

