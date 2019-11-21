package de.hpi.tdgt.activities;

import de.hpi.tdgt.Utils;
import de.hpi.tdgt.test.story.activity.Data_Generation;
import lombok.Setter;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;

public class TestDataGeneration {
    private Data_Generation firstGeneration;
    private Data_Generation secondGeneration;
    private Data_Generation thirdGeneration;
    private File users;
    private File posts;

    @BeforeEach
    public void beforeEach() throws IOException {
        firstGeneration = new Data_Generation();
        firstGeneration.setData(new String[]{"username", "password"});
        firstGeneration.setTable("users");
        secondGeneration = new Data_Generation();
        secondGeneration.setData(new String[]{"username", "password"});
        secondGeneration.setTable("users");


        thirdGeneration = new Data_Generation();
        thirdGeneration.setData(new String[]{"title", "text"});
        thirdGeneration.setTable("posts");


        users = new File("users.csv");
        users.deleteOnExit();
        var os = new FileOutputStream(users);
        IOUtils.copy(new Utils().getUsersCSV(), os);
        os.close();

        posts = new File("posts.csv");
        posts.deleteOnExit();
        os = new FileOutputStream(posts);
        IOUtils.copy(new Utils().getPostsCSV(), os);
        os.close();
    }
    @AfterEach
    public void afterEach(){

        //clear side effects
        Data_Generation.reset();
        System.out.println("Deleted users: "+users.delete());
        System.out.println("Deleted posts: "+posts.delete());
    }
    @Test
    public void firstGenerationShouldContainFirstElementOfUsersCSV() {
        Map<String, String> params = new HashMap<>();
        firstGeneration.run(params);
        params = firstGeneration.getKnownParams();
        assertThat(params, hasEntry("username", "AMAR.Aaccf"));
        assertThat(params, hasEntry("password", "Dsa9h"));
    }


    @Test
    public void firstGenerationShouldContainSecondElementOfUsersCSV() {
        Map<String, String> params = new HashMap<>();
        firstGeneration.run(params);
        firstGeneration.run(params);
        params = firstGeneration.getKnownParams();
        assertThat(params, hasEntry("username", "ATP.Aaren"));
        assertThat(params, hasEntry("password", "uwi4tQngkLL"));
    }

    @Test
    public void differentGenerationsProduceDifferentData() {
        Map<String, String> params = new HashMap<>();
        Map<String, String> otherParams = new HashMap<>();
        firstGeneration.run(params);
        params = firstGeneration.getKnownParams();
        secondGeneration.run(otherParams);
        otherParams = secondGeneration.getKnownParams();

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
    public void differentGenerationsWithSameTableProduceDifferentDataInDifferentThreads() throws InterruptedException {
        Map<String, String> params;
        Map<String, String> otherParams;
        val generationRunnable = runAsync(firstGeneration);
        val otherGenerationRunnable = runAsync(secondGeneration);
        generationRunnable.start();
        otherGenerationRunnable.start();
        generationRunnable.join();
        otherGenerationRunnable.join();
        params = firstGeneration.getKnownParams();
        otherParams = secondGeneration.getKnownParams();
        ArrayList<String> allValues = new ArrayList<>(params.values());
        allValues.addAll(otherParams.values());
        //We do not know in what sequence the Threads did run.
        //But one thread should have read the first line, the other thread the other line
        assertThat(allValues, containsInAnyOrder( "AMAR.Aaccf", "Dsa9h", "ATP.Aaren", "uwi4tQngkLL"));
    }

    @Test
    public void differentGenerationsWithDifferentTablesProduceDifferentDataInDifferentThreads() throws InterruptedException {
        Map<String, String> params;
        Map<String, String> otherParams;
        Map<String, String> thirdParams;
        val generationRunnable = runAsync(firstGeneration);
        val otherGenerationRunnable = runAsync(secondGeneration);
        val thirdGenerationRunnable = runAsync(thirdGeneration);
        generationRunnable.start();
        otherGenerationRunnable.start();
        thirdGenerationRunnable.start();
        generationRunnable.join();
        otherGenerationRunnable.join();
        thirdGenerationRunnable.join();
        params = firstGeneration.getKnownParams();
        otherParams = secondGeneration.getKnownParams();
        ArrayList<String> allValues = new ArrayList<>(params.values());
        allValues.addAll(otherParams.values());
        thirdParams = thirdGeneration.getKnownParams();
        //We do not know in what sequence the Threads did run.
        //But one thread should have read the first line, the other thread the other line
        assertThat(allValues, containsInAnyOrder( "AMAR.Aaccf", "Dsa9h", "ATP.Aaren", "uwi4tQngkLL"));
        assertThat(thirdParams, hasEntry("title","young boy becomes the cause of some real soul searching, as their family circle expanded to include the Custom House Essay,Hawthorne lays the plans for the beginning."));
        assertThat(thirdParams, hasEntry("text","$9 isn't too heavy to carry my pack with good digital camera kodak sells theDC3200.I bought this tripod for video tapping an upcomming family wedding and have tried it hundred times on newspaper up to $200 for it as being good tripod for stabilization and one on the small control buttons on the handle. The panning operation is likewise acceptable. The telescopic locking legs work well, but are light weight make it happen. This makes titling snap. Bundled software is easy to handle.The LCD is clear and easy to connect and engage. Remote control functions and handle are easy enough to prevent 'shaky hand', and not to bump the tripod warmed up in the box. Being an older Sony camera which liked most. without the use of the VCT-D680RM's remote control features. The DSC-N2 doesn't have good job.I would recommend kodak accessories for any kind of maddening to the PDA via the SD card...guess that's the case with most tiny cameras, and this records in SP (60 min for regular tapes) and LP (90 min), so you'll want to use which liked most. without the use of the tape in the video. \"Stiction\" is the 8-bit color. There are two problems. Check out the specifications and you need to edit your recordings it will NOT work with Cannon cameras.I like the problem with doing this as thought it would.I would buy this one. Recording quality is pretty good with an add-on high capacity battery. Best of all, you can get it for $10. Really what use the cards with RAW files and curves. It is what it is...and does what it is...and does what it was working fine, but the external microphone to remove the noise, in which PIXELLA converts the movie to MPEG. Dazzle USB brings in my movies that don't already have firewire interface and even though I'm only using the 460x digital zoom is fantastic, but you can plug it into the handle. The panning operation is likewise acceptable. The telescopic locking legs work well, but are light weight and size considerations and the computer asks if you are concerned about the camera with more high-end tripods. The other models looked at included the Sony VCT1170RM ($365) and far from Canon GL-1s or Xl-1s, but for what it was as good as radio or television.2) Solution from yet another user:Buy generic external mic, or Canon's DM-50 Microphone. This completely eliminates the motor noise, which is my first copy from the handle is easily removable and can afford DV or Digital 8mm camcorder. Yet.Enter the TRV-108, nifty menu system navigated by little disappointed when opened the package three credit card size pieces of plastic. My guess, and may well be wrong, is that one worked fine.I have two original Canon batteries, and now will take more videos of my camera, but they are with my Mac is on newspaper up to average bumps and knocks.....especially attached to the subject. The Canon brand is worth the extra cost."));
    }
}