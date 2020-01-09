package de.hpi.tdgt.controllers;

import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@Log4j2
public class UploadController {
    //will return 500 if exception during test occurs
    @PostMapping(path = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> uploadTestConfig(@RequestBody Test testToRun) throws InterruptedException, ExecutionException {
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
        return ret;
    }
}
