# Assembling the Fee Waiver Demo

*NOTE*: All instructions are provided for POSIX-based systems (Linux, MacOS). Instructions should be easily adaptable to Windows environments.

## Requirements

Here are the minimum requirements for the fee waiver demo application:

- Java 21
- Maven 3.9+
- Node.js (with npm) 18+

Please install these prerequisites using instructions available on the Internet before proceeding.

## Installing Ollama and Mistral Small

1. Install Ollama using this command: `curl -fsSL https://ollama.com/install.sh | sh`
2. Once installed, run Ollama: `ollama serve`
3. In a separate Terminal, pull the LLM: `ollama pull mistral-small`
4. In that same, separate Terminal, run the model: `ollama run mistral-small`

## Installing Fluxnova Modeler

Fluxnova Modeler can be installed through the App Store for MacOS or from the Windows Store for Microsoft. It can also be installed by following the instructions in the GitHub repo here: https://github.com/finos/fluxnova-modeler.

*NOTE*: The process model - which is entitled `process-fee-waiver-request.bpmn` - is available at `feewaiver-sb/src/main/resources`. That folder will exist once you've cloned the repo using the instructions under the "Clone the Repo" subheading below.

## Clone the Repo

Clone this GitHub repository using `git clone` and the URL/command shown within GitHub. This will give you access to this folder, which has the following structure:

- README.md: This is the `README.md ` file.
- feewaiver-sb: This is the Spring Boot project that contains both Fluxnova and the supporting backend code.
- feewaiver-ui: This contains the custom UI that can be used to view in progress or historic fee waiver requests.
- postman-requests: This folder contains a collection of Postman requests that can be used to initiate fee waiver requests. 

## Run the feewaiver-sb Spring Boot Project

1. In Terminal, navigate to the `feewaiver-sb` folder.
2. Build the project: `mvn clean package`.
3. Run the Spring Boot project: `mvn spring-boot:run`.

Once completed, the Fluxnova web apps (Monitoring, Tasklist and Admin) will be available at http://localhost:8080/fluxnova.

## Run the feewaiver-ui Custom User Interface

1. In Terminal, navigate to the `feewaiver-ui` folder.
2. Install the dependencies: `npm install`.
3. Run the UI: `npm run dev`.

Once completed, the UI will be available at http://localhost:5173.

## Run the Tests

1. Open your local copy of Postman.
2. Import the collection from the `postman-requests` folder; it's called "Fee Waiver REST Requests.postman_collection.json".
3. Run the request entitled "Start Process Fee Waiver Requests Human Review".
4. In the JSON response, find the `id` and copy that to your clipboard.
5. Open the feewaiver-ui custom user interface and paste the `id` value from step 4 into the box. You should see the running process instance.
6. While the process instance is still running, you can also view the process instance using Fluxnova Monitoring.
7. Once the instance reaches the User Task, navigate to Fluxnova Tasklist (http://localhost:8080/fluxnova/app/tasklist). Open the task ("Review Case"), mark it as approved by checking the checkbox next to "Review Decision? (Check if Approved)" and optionally provide your comments in the "Review Comments" box. Click "Complete", and the process instance will be completed.

If you'd prefer to use cURL or a different tool to issue the requests, you can simply open the Postman requests file in a text editor and use the information therein to craft your requests.

