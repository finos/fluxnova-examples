FROM amazoncorretto:21

# Setup required env variables
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto \
    PATH=$PATH:$JAVA_HOME/bin

WORKDIR /flowave

# Install aws-cli, jq, curl
RUN yum install -y tzdata jq curl bash

# Copies the Spring Boot fat JAR into the working directory
COPY target/flowave-springboot-example-*.jar flowave.jar

# Expose port
EXPOSE 8080
# Create user and provide access to flowave folder
RUN addgroup -S flowave_group -g 4001 && \
    adduser -S flowave_user -G flowave_group -u 4001 -h /flowave
RUN chown -R flowave_user:flowave_group /flowave

USER 4001
ENTRYPOINT ["java","-jar","flowave.jar"]
