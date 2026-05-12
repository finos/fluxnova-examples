# Assembling the Fee Waiver Demo

*NOTES*:

- All instructions are provided for POSIX-based systems (Linux, MacOS). Instructions should be easily adaptable to Windows environments.
- The included call to the LLM (Mistral Small through Ollama) takes some time; on a reasonably fast developer machine, please allow around 40 seconds for it to complete.

## Components

This demo includes the following components:

1. Fluxnova Spring Boot app (`feewaiver-sb`): This Spring Boot app starts Fluxnova and provides the custom back-end code for this demo.
2. Summit58 Fee Waiver instance & history viewer (`feewaiver-ui`): This npm app contains a custom-developed user interface that demonstrates how the bpmn-js library (https://github.com/bpmn-io/bpmn-js) can be used to view running and historic process instances. Whereas Fluxnova Monitoring only shows running process instances, this custom user interface also shows completed, historic process instances.
3. Postman Requests (`postman-requests`): This folder contains a single collection of Postman requests that can be used to run the Fee Waiver demo. *NOTE*: An example cURL command is also provided in the "Sample Request Using cURL" section below.
4. Ollama & Mistral Small: This is a separate dependency that isn't included in this repo; it must be installed using the instructions in the section entitled "Installing Ollama and Mistral Small" below. Once installed, no modifications or configuration changes to this component are required.

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

Fluxnova Modeler can be installed through the App Store for MacOS or the Windows Store for Microsoft. It can also be installed by following the instructions in the GitHub repo here: https://github.com/finos/fluxnova-modeler.

*NOTE*: The process model - which is entitled `process-fee-waiver-request.bpmn` - is available at `feewaiver-sb/src/main/resources`. That folder will exist once you've cloned the repo using the instructions under the "Clone the Repo" subheading below.

## Clone the Repo

Clone this GitHub repository using `git clone` and the URL/command shown within GitHub. This will give you access to the source code for the demo, which is provided using the following structure:

- README.md
- feewaiver-sb
- feewaiver-ui
- postman-requests 

Please reference the "Components" section above for more information on each of these subfolders.

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

1. Open your local copy of Postman OR reference the provided cURL example in the "Sample Request Using cURL" section below. If using Postman, go to step 2. If using cURL, once you've issued the cURL command shown in the "Sample Request Using cURL" section below, please go to step 4.
2. Import the collection from the `postman-requests` folder; it's called "Fee Waiver REST Requests.postman_collection.json".
3. Run the request entitled "Start Process Fee Waiver Requests Human Review". 
4. In the JSON response, find the `id` and copy that to your clipboard.
5. Open the feewaiver-ui custom user interface, paste the `id` value from step 4 into the box and click the "Load" button. You should see the running process instance.
6. *NOTE*: The included call to the LLM (`mistral-small` through Ollama) takes some time; on a reasonably fast developer machine, please allow around 40 seconds for it to complete.
7. While the process instance is still running, you can also view the process instance using Fluxnova Monitoring.
8. Once the instance reaches the User Task, navigate to Fluxnova Tasklist (http://localhost:8080/fluxnova/app/tasklist). Open the task ("Review Case"), mark it as approved by checking the checkbox next to "Review Decision? (Check if Approved)" and optionally provide your comments in the "Review Comments" box. Click "Complete", and the process instance will be completed.

## Sample Request Using cURL

Here's the cURL command you can use at the command-line to kick off an instance of the `process-fee-waiver-request` model with the same payload that is used in the Postman request entitled "Start Process Fee Waiver Requests Human Review":

```Bash
curl -X POST "http://localhost:8080/engine-rest/process-definition/key/process-fee-waiver-request/start" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
        "variables": {
                "requestText": {
                        "value": "I need all $650 in overdraft and related fees reversed. This has happened multiple times because of issues with my account. Please fix it today.",
                        "type":"String"
                },
                "priorFeeWaiverCount": {
                        "value": 3,
                        "type":"Long"
                },
                "feeAmount": {
                        "value": 650.0,
                        "type":"Double"
                },
                "vulnerableCustomer": {
                        "value": false,
                        "type":"Boolean"
                },
                "regulatorySensitivity": {
                        "value": false,
                        "type":"Boolean"
                }
        },
        "businessKey":"test1"
}'
```

If you'd prefer to use cURL to issue all the requests, you can simply open the included Postman requests file ("Fee Waiver REST Requests.postman_collection.json") in a text editor and use the information in each request along with the example above to craft the proper cURL command in each case.

