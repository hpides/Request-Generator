# Request Generator

Component responsible for generation, sending and performance evaluation of requests.

The code offers a web server mode of operation (production, with frontend) and a single-command cli mode for testing.

## How to execute (Command Line only)
You can run a test like this:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --load ./src/test/resources/de/hpi/tdgt/test_config_example.json ./src/test/resources/de/hpi/tdgt/ java7
```
First argument (--load) is the test file to run, second the directory where the data for the test are, third is the path of the Java interpreter as e.g. execve expects it.
Users can specify a threshold for the response time of all sent requests that if exceeded lets Request Generator exit with return code 3 instead of 0 in the test configuration (attribute *requestDurationThreshold*). If an assertion in the test fails, Request Generator will exit the command line mode with return code 2. If both assertions fail and the threshold is exceeded, it will exit with return code 5.

## How to execute (Web Server)
Start the webserver with one argument that states where PDGF should put test data and one with the java 7 interpreter path as above, e.g.:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --location http://localhost:8080 ./src/test/resources/de/hpi/tdgt/ java7
```
You can execute tests using curl, e.g.:
```bash
curl -H "Content-Type: application/json" -X POST --data @src/test/resources/de/hpi/tdgt/test_config_example.json localhost:8080/upload/0
```
0 might be replaced by the current epoch time in milliseconds.

## Changing mqtt broker location

The broker location is per default derived from the application.properties configuration file. However, it can be overwritten during run time using --broker-url. Refer to --help for documentation.

## Testing correct syntax
Request Generator accepts an additional flag --noop. If used, it will check if sufficient parameters have been supplied but not run any tests. Note that this does not check the validity of supplied parameters, e.g. if passed pathnames exist.

## A note on logging
The default logging level is "info". It will provide a lot of output which will slow the application down. Feel free to overwrite "logging.level.root=info" in application.properties with "warn" or "error".  
Also, the logging level can be set at run time via the argument "--logging.level.root="error"".  
Output of times will happen at the error level.  
Also, all other Spring Boot arguments should work as expected.

## A note on distributed usage
To use the distribution feature, connect as many nodes as you like to the same broker.   
Each node should have a private PDGF instance (because data of the same name might have to be generated).   
Also, at startup a node needs to be told it's URL relative to the other nodes (parameter --location).   
The frontend needs to be able to contact one of the nodes (there is no dedicated master node).  
If these steps are followed, the system should automatically distribute benchmarking to all nodes in the system.  
Finally, this system can not tolerate adding nodes after the PDGF run of a test (re-run the PDGF step in this case) or losing nodes during the test run (test will never finish).
