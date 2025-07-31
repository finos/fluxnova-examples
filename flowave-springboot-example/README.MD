
## Example Flowave Spring Boot application

This is a sample Spring Boot application demonstrating the integration of all Flowave dependencies. You can use this project to run a Spring Boot application locally and access the Flowave application.

## Prerequisites

- Java 21 or later
- Maven 3.6.x or later

## Getting Started

1. **Build the `flowave-bpm-platform` project locally. Follow the instructions in the `flowave-build-readme.md` file to build the project.**
2. **Clone the repository** (if you haven't already):

   ```sh
   git clone https://github.com/finos/flowave-examples.git
   ```

3. **Build the flowave springboot example project:**

   ```sh
   cd flowave-examples/flowave-springboot-example
   mvn clean install
   ```

4. **Run the Spring Boot application:**

   ```sh
   mvn spring-boot:run
   ```

5. **Access the Flowave application:**

   Open your browser and go to:  
   [http://localhost:8080](http://localhost:8080)

## Notes

- Make sure all Flowave dependencies are present in the `pom.xml`.
- The application runs on port `8080` by default. You can change this in `application.yaml` if needed.

## Troubleshooting

- If you encounter port conflicts, ensure nothing else is running on port `8080`.
- For Maven or Java issues, verify your environment variables and tool versions.