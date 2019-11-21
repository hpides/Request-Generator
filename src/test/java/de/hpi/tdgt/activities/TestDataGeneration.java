package de.hpi.tdgt.activities;

import de.hpi.tdgt.Utils;
import de.hpi.tdgt.test.story.activity.Data_Generation;
import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;

public class TestDataGeneration {
    private Data_Generation generation;
    private Data_Generation otherGeneration;

    @BeforeEach
    public void beforeAll() throws IOException {
        generation = new Data_Generation();
        generation.setStream(new Utils().getUsersCSV());
        generation.setData(new String[]{"username", "password"});
        generation.setTable("users");
        otherGeneration = new Data_Generation();
        otherGeneration.setStream(new Utils().getUsersCSV());
        otherGeneration.setData(new String[]{"username", "password"});
        otherGeneration.setTable("users");
    }

    @Test
    public void generationShouldContainFirstElementOfUsersCSV() {
        Map<String, String> params = new HashMap<>();
        generation.run(params);
        params = generation.getKnownParams();
        assertThat(params, hasEntry("username", "AMAR.Aaccf"));
        assertThat(params, hasEntry("password", "Dsa9h"));
    }

    @Test
    public void generationShouldContainSecondElementOfUsersCSV() {
        Map<String, String> params = new HashMap<>();
        generation.run(params);
        generation.run(params);
        params = generation.getKnownParams();
        assertThat(params, hasEntry("username", "ATP.Aaren"));
        assertThat(params, hasEntry("password", "uwi4tQngkLL"));
    }

    @Test
    public void differentGenerationsProduceDifferentData() {
        Map<String, String> params = new HashMap<>();
        Map<String, String> otherParams = new HashMap<>();
        generation.run(params);
        params = generation.getKnownParams();
        otherGeneration.run(otherParams);
        otherParams = otherGeneration.getKnownParams();

        assertThat(params, hasEntry("username", "AMAR.Aaccf"));
        assertThat(params, hasEntry("password", "Dsa9h"));
        assertThat(otherParams, hasEntry("username", "ATP.Aaren"));
        assertThat(otherParams, hasEntry("password", "uwi4tQngkLL"));
    }

    private class DataGenRunnable implements Runnable {
        @Setter
        private Data_Generation gen;

        @Override
        public void run() {
            gen.run(new HashMap<>());
        }
    }

    private Thread runAsync(Data_Generation generation) {
        DataGenRunnable ret = new DataGenRunnable();
        ret.setGen(generation);
        return new Thread(ret);
    }

    @Test
    public void differentGenerationsProduceDifferentDataInDifferentThreads() throws InterruptedException {
        Map<String, String> params;
        Map<String, String> otherParams;
        val generationRunnable = runAsync(generation);
        val otherGenerationRunnable = runAsync(otherGeneration);
        generationRunnable.start();
        otherGenerationRunnable.start();
        generationRunnable.join();
        otherGenerationRunnable.join();
        params = generation.getKnownParams();
        otherParams = otherGeneration.getKnownParams();
        ArrayList<String> allValues = new ArrayList<>(params.values());
        allValues.addAll(otherParams.values());
        //We do not know in what sequence the Threads did run.
        //But one thread should have read the first line, the other thread the other line
        assertThat(allValues, containsInAnyOrder( "AMAR.Aaccf", "Dsa9h", "ATP.Aaren", "uwi4tQngkLL"));
    }
}
