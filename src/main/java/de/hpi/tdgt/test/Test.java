package de.hpi.tdgt.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import de.hpi.tdgt.test.story.atom.WarmupEnd;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.PropertiesReader;
import jdk.jshell.spi.ExecutionControl;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
public class Test {
    /**
     * Topic on which control messages are broadcasted
     */
    public static final String MQTT_TOPIC = "de.hpi.tdgt.control";
    //stories need to know the test e.g. for it's id
    public void setStories(UserStory[] stories) {
        this.stories = stories;
        Arrays.stream(stories).forEach(story -> story.setParent(this));
    }

    /**
     * Contains the original test config as JSON. This saves time because when broadcasting it, the test does not have to be serialized again.
     */
    private String configJSON;
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;
    //this is used to be able to repeat them
    private UserStory[] stories_clone;
    private int activeInstancesPerSecond = DEFAULT_ACTIVE_INSTANCES_PER_SECOND_LIMIT;
    private MqttClient client;

    public static final int DEFAULT_CONCURRENT_REQUEST_LIMIT = 100;
    public static final int DEFAULT_ACTIVE_INSTANCES_PER_SECOND_LIMIT = 10000;

    //by default, do not limit number of concurrent requests
    private int maximumConcurrentRequests = DEFAULT_CONCURRENT_REQUEST_LIMIT;

    @Getter
    @Setter
    @JsonIgnore
    //we can assume this is unique. Probably, only one test at a time is run.
    private long testId = System.currentTimeMillis();

    /**
     * Perform deep clone of stories to user_stories.
     */
    private void cloneStories(UserStory[] source, UserStory[] target){
        for(int i=0; i < source.length; i++){
            target[i] = source[i].clone();
        }
    }

    /**
     * Run all stories that have WarmupEnd until reaching WarmupEnd. Other stories are not run.
     * *ALWAYS* run start after running warmup before you run warmup again, even in tests, to get rid of waiting threads.
     * @return threads in which the stories run to join later
     * @throws InterruptedException if interrupted in Thread.sleep
     */
    public Collection<Future<?>> warmup() throws InterruptedException {
        //preserve stories for test repetition
        stories_clone = new UserStory[stories.length];
        cloneStories(stories, stories_clone);
        ActiveInstancesThrottler.setInstance(this.activeInstancesPerSecond);
        Thread watchdog = new Thread(ActiveInstancesThrottler.getInstance());
        watchdog.setPriority(Thread.MAX_PRIORITY);
        watchdog.start();
        //will run stories with warmup only, so they can run until WarmupEnd is reached
        val threads = runTest(Arrays.stream(stories).filter(UserStory::hasWarmup).toArray(UserStory[]::new));
        //now, wait for all warmups to finish

        //casting to int clears decimals
        int waitersToExpect = Arrays.stream(stories).mapToInt(story -> (int)(story.numberOfWarmupEnds() * story.getScalePercentage() * scaleFactor)).sum();
        //wait for all warmup ends to be stuck
        while(waitersToExpect > WarmupEnd.getWaiting()){
            log.info("Waiting for warmup to complete: "+WarmupEnd.getWaiting() + " of "+waitersToExpect+" complete!");
            Thread.sleep(5000);
        }
        watchdog.interrupt();
        return threads;
    }

    /**
     * Use this if you do not have threads from warmup.
     * @throws InterruptedException if interrupted joining threads
     */
    public void start() throws InterruptedException, ExecutionException {
        //preserve stories for repeat
        stories_clone = new UserStory[stories.length];
        cloneStories(stories, stories_clone);
        start(new Vector<>());
    }

    /**
     * Use this method to wait for threads left from warmup.
     * @param threadsFromWarmup Collection of threads to wait for
     * @throws InterruptedException if interrupted joining threads
     */
    public void start(Collection<Future<?>> threadsFromWarmup) throws InterruptedException, ExecutionException {
        prepareMqttClient();
        try {
            //clear retained messages from last test
            client.publish(MQTT_TOPIC, new byte[0],0,true);
            client.publish(MQTT_TOPIC, ("testStart "+testId+" "+configJSON).getBytes(StandardCharsets.UTF_8),2,false);
        } catch (MqttException e) {
            log.error("Could not send control start message: ", e);
        }

        for(int i = 0; i < repeat; i++) {
            log.info("Starting test run "+i+" of "+repeat);
            //start all warmup tasks
            WarmupEnd.startTest();
            //this thread makes sure that requests per second get limited
            ActiveInstancesThrottler.setInstance(this.activeInstancesPerSecond);
            Thread watchdog = new Thread(ActiveInstancesThrottler.getInstance());
            watchdog.setPriority(Thread.MAX_PRIORITY);
            watchdog.start();
            val threads = runTest(Arrays.stream(stories).filter(story -> !story.isStarted()).toArray(UserStory[]::new));
            //can wait for these threads also
            threads.addAll(threadsFromWarmup);
            for (val thread : threads) {
                //join thread
                if (!thread.isCancelled())
                    thread.get();
            }
            watchdog.interrupt();
            //remove global state
            ActiveInstancesThrottler.reset();
            Data_Generation.reset();
            //this resets all state atoms might have
            cloneStories(stories_clone, stories);
            //do not run another warmup after the last run, because it would not be finished
            if(i < repeat - 1) {
                threadsFromWarmup = warmup();
            }
        }
        //make sure all times are sent
        AssertionStorage.getInstance().flush();
        TimeStorage.getInstance().flush();


        try {
            client.publish(MQTT_TOPIC, ("testEnd "+testId).getBytes(StandardCharsets.UTF_8),2,false);
            //clear retained messages for next test
            client.publish(MQTT_TOPIC, new byte[0],0,true);
        } catch (MqttException e) {
            log.error("Could not send control end message: ", e);
        }
        try {
            client.disconnect();
        } catch (MqttException e) {
            log.warn("Could not disconnect client: ",e);
        }
    }

    private void prepareMqttClient() {
        if (client == null || ! client.isConnected()) {
            try {
                String publisherId = UUID.randomUUID().toString();
                //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                client = new MqttClient(PropertiesReader.getMqttHost(), publisherId, new MemoryPersistence());
            } catch (MqttException e) {
                log.error("Error creating mqttclient in AssertionStorage: ", e);
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            try {
                client.connect(options);
                //clear retained messages from last test
                client.publish(MQTT_TOPIC, new byte[0],0,true);
            } catch (MqttException e) {
                log.error("Could not connect to mqtt broker in AssertionStorage: ", e);
            }
        }
    }

    private Collection<Future<?>> runTest(UserStory[] stories) throws InterruptedException {
        try {
            ConcurrentRequestsThrottler.getInstance().setMaxParallelRequests(maximumConcurrentRequests);
        } catch (ExecutionControl.NotImplementedException e) {
            log.error(e);
        }
        val futures = new Vector<Future<?>>();
        for(int i=0; i < stories.length; i++){
            //repeat stories as often as wished
            for(int j = 0; j < scaleFactor * stories[i].getScalePercentage(); j++) {
                stories[i].setParent(this);
                stories[i].setStarted(true);
                val future = ThreadRecycler.getInstance().getExecutorService().submit(stories[i]);
                futures.add(future);
            }
        }
        return futures;
    }

    /**
     * This is a runnable for a high-priority thread that makes sure that no more user stories than requested run.
     */
    @Log4j2
    public static class ActiveInstancesThrottler implements Runnable{
        static void setInstance(int instancesPerSecond){
            instance = new ActiveInstancesThrottler(instancesPerSecond);
        }
        @Getter
        private static ActiveInstancesThrottler instance = null;

        private ActiveInstancesThrottler(int instancesPerSecond){
            requestLimiter =  new Semaphore(instancesPerSecond,true);
        }
        //only used for measurement, does not have to be synchronized
        @Getter
        int instancesPerSecond = 0;
        private static void reset(){
            instance = null;
        }
        /**
         * Stops threads from increasing requests per second while re-creating tickets
         */
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore requestLimiter;
        public void allowInstanceToRun() throws InterruptedException {
            log.trace("Waiting for requestLimiter...");
            requestLimiter.acquire();
            log.trace("Waiting for mutex (allowRequest)...");
            mutex.acquire();
            instancesPerSecond++;
            mutex.release();
            log.trace("Released mutex (allowRequest)");
        }
        @Override
        public void run() {
            while(!Thread.interrupted()) {
                int instancesLastSecond = instancesPerSecond;
                log.trace("Waiting for mutex (run)...");
                log.info(instancesLastSecond+" active instances in the last second.");
                try {
                    mutex.acquire();
                    requestLimiter.release(instancesPerSecond);
                    instancesPerSecond = 0;
                    mutex.release();
                    log.trace("Released mutex (run)");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
            log.trace("Requests per second watchdog was interrupted!");
        }
    }

    /**
     * This makes sure not more requests than configured run in parallel
     */
    public static class ConcurrentRequestsThrottler {
        @Getter
        private static final ConcurrentRequestsThrottler instance = new ConcurrentRequestsThrottler();

        private Semaphore maxParallelRequests;
        private int waiters = 0;
        private int active = 0;

        @Getter
        int maximumParallelRequests = 0;
        public void setMaxParallelRequests(int concurrent) throws ExecutionControl.NotImplementedException {
            if(maxParallelRequests == null) {
                maxParallelRequests = new Semaphore(concurrent);
                active = 0;
                maximumParallelRequests = 0;
            }
            else {
                int waiters;
                synchronized (this) {
                    waiters = this.waiters;
                    //we needed to wait until no thread is waiting for the semaphore
                    if (waiters > 0) {
                        throw new ExecutionControl.NotImplementedException("Can not change number of concurrent requests while there are threads waiting for the semaphore!");
                    }
                    maxParallelRequests = new Semaphore(concurrent);
                    active = 0;
                    maximumParallelRequests = 0;
                }
            }
        }

        public void allowRequest() throws InterruptedException {
                synchronized (this){
                    waiters++;
                }
                if(maxParallelRequests != null) {
                    maxParallelRequests.acquire();
                }
                synchronized (this){
                    waiters --;
                    active++;
                    //used by the test of this feature
                    if(active > maximumParallelRequests){
                        maximumParallelRequests = active;
                    }
                }
        }

        public void requestDone(){
            if(maxParallelRequests != null) {
                maxParallelRequests.release();
            }
            synchronized (this){
                active--;
            }

        }

        public void reset(){
            maxParallelRequests = null;
            active = 0;
            maximumParallelRequests = 0;
        }

    }
}
