# Request Generator

Component responsible for generation, sending and performance evaluation of requests.

The code offers a web server mode of operation (production, with frontend) and a single-command cli mode for testing.

## How to execute (Command Line only)
You can run a test like this:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar cli load ./src/test/resources/de/hpi/tdgt/test_config_example.json ./src/test/resources/de/hpi/tdgt/
```
First argument is the test file to run, second the directory where the data for the test are.
## How to execute (Web Server)
Start the webserver with one argument that states where PDGF should put test data, e.g.:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar ./src/test/resources/de/hpi/tdgt/
```
You can execute tests using curl, e.g.:
```bash
curl -H "Content-Type: application/json" -X POST --data @src/test/resources/de/hpi/tdgt/test_config_example.json localhost:8080/upload/0
```
0 might be replaced by the current epoch time in milliseconds.
