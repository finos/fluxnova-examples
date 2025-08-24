FROM amazoncorretto:21

# Setup required env variables
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto \
    PATH=$PATH:$JAVA_HOME/bin

WORKDIR /flowave

# Install packages
RUN yum install -y jq curl shadow-utils

# Copies the Spring Boot fat JAR into the working directory
COPY flowave-springboot-example/target/flowave-springboot-example-*.jar flowave.jar

# Expose port
EXPOSE 8080
# Create user and provide access to flowave folder
RUN groupadd -g 4001 flowave_group && adduser -G flowave_group -u 4001 -m -d /flowave flowave_user
RUN chown -R flowave_user:flowave_group /flowave

USER 4001
ENTRYPOINT ["java","-jar","flowave.jar"]
