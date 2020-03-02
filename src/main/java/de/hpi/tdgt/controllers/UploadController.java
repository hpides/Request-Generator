package de.hpi.tdgt.controllers;

import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.requesthandling.RestClient;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@RestController
@Log4j2
public class UploadController {
    //will return 500 if exception during test occurs
    @PostMapping(path = "/upload/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> uploadTestConfig(@RequestBody String testToRunAsJSON, @PathVariable(required = false) long id) throws InterruptedException, ExecutionException {
        Test testToRun;
        try {
            testToRun = Deserializer.deserialize(testToRunAsJSON);
        } catch (IOException e) {
            log.error(e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        testToRun.setTestId(id);
        val ret  = new ResponseEntity<String>(HttpStatus.OK);
        long starttime = System.currentTimeMillis();
        val threads = testToRun.warmup();
        testToRun.start(threads);
        long endtime = System.currentTimeMillis();
        log.info("---Test finished in "+(endtime - starttime)+" ms.---");
        log.info("---Times---");
        TimeStorage.getInstance().printSummary();
        log.info("---Assertions---");
        AssertionStorage.getInstance().printSummary();
        TimeStorage.getInstance().reset();
        AssertionStorage.getInstance().reset();
        log.info(RestClient.requestsSent.get()+" requests sent.");
        RestClient.requestsSent.set(0);
        return ret;
    }

    public static String JAVA_7_DIR = null;
    public static String PDGF_DIR = null;

    //will return 500 if exception during test occurs
    @PostMapping(path = "/uploadPDGF", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> uploadDataGenConfig(@RequestBody String pdgfConfig) throws InterruptedException, ExecutionException {
        val ret  = new ResponseEntity<String>(HttpStatus.OK);
        long starttime = System.currentTimeMillis();
        //store uploaded config in temporary file, so multiple instances could run concurrently
        File tempFile;
        try {
            tempFile = File.createTempFile("pdgf", ".xml");
            tempFile.deleteOnExit();
            val writer = new FileWriter(tempFile);
            writer.write(pdgfConfig);
            writer.close();
        } catch (IOException e) {
            log.error(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            val pdgfProcess = new ProcessBuilder(JAVA_7_DIR, "-jar",PDGF_DIR+ File.separator+"pdgf.jar", "-l", tempFile.getAbsolutePath(),  "-l", PDGF_DIR+File.separator+"config"+File.separator+"customer-output.xml", "-c", "-ns", "-s").start();
            log.info("PDGF command: {}", (Object) new String[]{JAVA_7_DIR, "-jar",PDGF_DIR+ File.separator+"pdgf.jar", "-l", tempFile.getAbsolutePath(),  "-l", PDGF_DIR+File.separator+"config"+File.separator+"customer-output.xml", "-c", "-ns", "-s"});
            log.info(pdgfProcess.info());
            try(val input = new BufferedReader(new InputStreamReader(pdgfProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                log.info("PDGF Output:");
                while ((line = input.readLine()) != null) {
                    log.info(line);
                }
            }
            //wait for process to terminate
            val returnCode = pdgfProcess.waitFor();
            if(returnCode != 0){
                log.error("PDGF exited with "+returnCode);
            }
        } catch (IOException e) {
            log.error(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        long endtime = System.currentTimeMillis();
        log.info("---Data Generation finished in "+(endtime - starttime)+" ms.---");
        return ret;
    }
}
