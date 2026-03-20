## Example Fluxnova Spring Boot application

<!-- Table of Contents -->
## Table of Contents
- [Prerequisites](#prerequisites)
- [Building the Spring Boot Application Locally](#building-the-spring-boot-application-locally)
- [Using the Published Docker Image](#using-the-published-docker-image)
- [Running the Spring Boot Application Using the Jar from GitHub Packages](#running-the-spring-boot-application-using-the-jar-from-github-packages)
- [Notes](#notes)
- [Troubleshooting](#troubleshooting)

This is a sample Spring Boot application demonstrating the integration of all Fluxnova dependencies. You can use this project to run a Spring Boot application locally and access the Fluxnova application.

**Note:**  
This example Spring Boot application includes dependencies for the HTTP connector and script engine. You can use JavaScript or Groovy scripts within your workflows.

## Prerequisites

- Java 21 or later
- Maven 3.6.x or later

## Building the Spring Boot Application Locally

1. **Clone the repository** (if you haven't already):

   ```sh
   git clone https://github.com/finos/fluxnova-examples.git
   ```

2. **Build the fluxnova springboot example project:**

   ```sh
   cd fluxnova-examples/fluxnova-springboot-example
   mvn clean install
   ```

3. **Run the Spring Boot application:**

   ```sh
   mvn spring-boot:run
   ```

4. **Access the Fluxnova application:**

   Open your browser and go to:  
   [http://localhost:8080](http://localhost:8080)

**Note:**  
By default, the application uses an H2 database. All Spring Boot configurations can be found in the `application.yaml` file. You can update these configurations as needed.

## Using the Published Docker Image

1. **Pull the Docker image:**

   ```sh
   docker pull finos/fluxnova-examples:1.0.0
   ```

2. **Run the Docker image with host port 8080:**

   ```sh
   docker run -p 8080:8080 finos/fluxnova-examples:1.0.0
   ```

3. **Database Configuration:**

   - By default, the Spring Boot application uses an H2 database.
   - It also supports PostgreSQL and MySQL databases.
   - To connect to PostgreSQL or MySQL, pass the following environment variables when starting the container:

     ```
     DB_DRIVER=<db-driver>
     DB_USERNAME=<username>
     DB_URL=<db-url>/<db-name>
     DB_PASSWORD=<password>
     ```

   - You can pass any Spring Boot configuration as environment variables when starting the Docker container.

4. **Example: Run the Docker image with custom database environment variables and a Spring Boot configuration**

   ```sh
   docker run -p 8080:8080 \
     -e DB_DRIVER=<db-driver> \
     -e DB_USERNAME=<username> \
     -e DB_URL=<db-url>/<db-name> \
     -e DB_PASSWORD=<password> \
     -e FLUXNOVA_BPM_ADMIN_USER_ID=demo \
     -e FLUXNOVA_BPM_ADMIN_USER_PASSWORD=demo \
     finos/fluxnova-examples:1.0.0
   ```

   Here, `FLUXNOVA_BPM_ADMIN_USER_ID` and `FLUXNOVA_BPM_ADMIN_USER_PASSWORD` override the default admin user credentials (`fluxnova.bpm.admin-user.id` and `fluxnova.bpm.admin-user.password`) in the application.

5. **Access the application:**

   Open your browser and go to:  
   [http://localhost:8080](http://localhost:8080)

## Running the Spring Boot Application Using the Jar from GitHub Packages

You can download the pre-built jar from GitHub Packages and run the application directly.

1. **Download the jar from GitHub Packages:**

   - Go to: [https://github.com/finos/fluxnova-examples/packages](https://github.com/finos/fluxnova-examples/packages)
   - Locate the latest `fluxnova-springboot-example` jar
   - Download the jar file to your local machine.

2. **Run the jar:**

   ```sh
   java -jar fluxnova-springboot-example-<version>.jar
   ```

   Replace `<version>` with the actual version of the jar you downloaded.

3. **Access the Fluxnova application:**

   Open your browser and go to:  
   [http://localhost:8080](http://localhost:8080)

**Note:**  
You can override Spring Boot configuration properties using command line arguments or environment variables as needed.  
For example, to set the admin user credentials:

```sh
java -jar fluxnova-springboot-example-<version>.jar \
  --fluxnova.bpm.admin-user.id=demo \
  --fluxnova.bpm.admin-user.password=demo
```

## Notes

- Make sure all Fluxnova dependencies are present in the `pom.xml`.
- The application runs on port `8080` by default. You can change this in `application.yaml` if needed.

## Troubleshooting

- If you encounter port conflicts, ensure nothing else is running on port `8080`.
- For Maven or Java issues, verify your environment variables and tool versions.