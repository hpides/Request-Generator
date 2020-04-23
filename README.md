# Request Generator

Component responsible for generation, sending and performance evaluation of requests.

The code offers a web server mode of operation (production, with frontend) and a single-command cli mode for testing.

## Setup

This project imports the stats project as an artifact via the GitLab maven repo. Currently, this does no work without authentication. To be able to build this project, you will need to create a personal access token with full "api" scope at [https://gitlab.hpi.de/profile/personal_access_tokens](https://gitlab.hpi.de/profile/personal_access_tokens) . After this, save this as ~/.m2/settings.xml:
```xml
<settings>
  <servers>
    <server>
      <id>gitlab-maven</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
              <value>YOUR TOKEN HERE</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>
</settings>
```
Note that the Docker build script will expect a copy of this file in the same directory as pom.xml of this project, because the maven build also relies on it for authentication. The settings.xml will not be present in the final container.  
Hint: If you added the settings.xml and build still fails, try
```bash
mvn clean -U package -DskipTests
```
## How to execute (Command Line only)
You can run a test like this:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --load ./src/test/resources/de/hpi/tdgt/test_config_example.json ./src/test/resources/de/hpi/tdgt/ java7
```
First argument is the test file to run, second the directory where the data for the test are, third is the path of the Java interpreter as e.g. execve expects it.
## How to execute (Web Server)
Start the webserver with one argument that states where PDGF should put test data and one with the java 7 interpreter path as above, e.g.:
```bash
java -jar request-generator-1.0-SNAPSHOT-jar-with-dependencies.jar ./src/test/resources/de/hpi/tdgt/ java7
```
You can execute tests using curl, e.g.:
```bash
curl -H "Content-Type: application/json" -X POST --data @src/test/resources/de/hpi/tdgt/test_config_example.json localhost:8080/upload/0
```
0 might be replaced by the current epoch time in milliseconds.

## A note on logging
The default logging level is "info". It will provide a lot of output which will slow the application down. Feel free to overwrite "logging.level.root=info" in application.properties with "warn" or "error".  
Also, the logging level can be set at run time via the argument "--logging.level.root="error"".  
Output of times will happen at the error level.

## A note on distributed usage
To use the distribution feature, connect as many nodes as you like to the same broker.   
Each node should have a private PDGF instance (because data of the same name might have to be generated).   
Also, at startup a node needs to be told it's URL relative to the other nodes (parameter --location).   
The frontend needs to be able to contact one of the nodes (there is no dedicated master node).  
If these steps are followed, the system should automatically distribute benchmarking to all nodes in the system.  
Finally, this system can not tolerate adding nodes after the PDGF run of a test (re-run the PDGF step in this case) or losing nodes during the test run (test will never finish).
