package de.hpi.tdgt.test;

import co.paralleluniverse.fibers.Fiber;
import de.hpi.tdgt.test.story.atom.WarmupEnd;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import co.paralleluniverse.strands.concurrent.Semaphore;
import java.util.stream.Collectors;

import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;
import lombok.val;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
public class Test {
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;
    private int requests_per_second;

    /**
     * Run all stories that have WarmupEnd until reaching WarmupEnd. Other stories are not run.
     * *ALWAYS* run start after running warmup before you run warmup again, even in tests, to get rid of waiting threads.
     * @return threads in which the stories run to join later
     * @throws InterruptedException if interrupted in Thread.sleep
     */
    public Collection<Fiber<Void>> warmup() throws InterruptedException {
        //might have been used by a previous test
        AssertionStorage.getInstance().reset();
        TimeStorage.getInstance().reset();
        
        RequestThrottler.setInstance(this.requests_per_second);
        Thread watchdog = new Thread(RequestThrottler.getInstance());
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
        start(new Vector<>());
    }

    /**
     * Use this method to wait for threads left from warmup.
     * @param threadsFromWarmup Collection of threads to wait for
     * @throws InterruptedException if interrupted joining threads
     */
    public void start(Collection<Fiber<Void>> threadsFromWarmup) throws InterruptedException, ExecutionException {
        //start all warmup tasks
        WarmupEnd.startTest();
        //this thread makes sure that requests per second get limited
        RequestThrottler.setInstance(this.requests_per_second);
        Thread watchdog = new Thread(RequestThrottler.getInstance());
        watchdog.setPriority(Thread.MAX_PRIORITY);
        watchdog.start();
        val threads = runTest(Arrays.stream(stories).filter(story -> !story.isStarted()).toArray(UserStory[]::new));
        //can wait for these threads also
        threads.addAll(threadsFromWarmup);
        for(Fiber<Void> thread : threads){
            //join thread
            if(!thread.isCancelled())
                thread.get();
        }
        watchdog.interrupt();
        //remove global state
        RequestThrottler.reset();
    }

    private Collection<Fiber<Void>> runTest(UserStory[] stories) throws InterruptedException {
        val futures = new Vector<Fiber<Void>>();
        for(int i=0; i < stories.length; i++){
            //repeat stories as often as wished
            for(int j = 0; j < scaleFactor * stories[i].getScalePercentage(); j++) {
                stories[i].setStarted(true);
                val future = new Fiber<Void>(stories[i]);
                futures.add(future);
            }
        }
        return futures;
    }

    /**
     * This is a runnable for a high-priority thread that makes sure that no more threads than requested run.
     */
    @Log4j2
    public static class RequestThrottler implements Runnable{
        static void setInstance(int requestsPerSecond){
            instance = new RequestThrottler(requestsPerSecond);
        }
        @Getter
        private static RequestThrottler instance = null;

        private RequestThrottler(int requestsPerSecond){
            requestLimiter =  new Semaphore(requestsPerSecond,true);
        }
        //only used for measurement, does not have to be synchronized
        @Getter
        int requestsPerSecond = 0;
        private static void reset(){
            instance = null;
        }
        /**
         * Stops threads from increasing requests per second while re-creating tickets
         */
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore requestLimiter;
        public void allowRequest() throws InterruptedException {
            log.trace("Waiting for requestLimiter...");
            requestLimiter.acquire();
            log.trace("Waiting for mutex (allowRequest)...");
            mutex.acquire();
            requestsPerSecond++;
            mutex.release();
            log.trace("Released mutex (allowRequest)");
        }
        @Override
        public void run() {
            while(!Thread.interrupted()) {
                int requests_lastsecond = requestsPerSecond;
                log.trace("Waiting for mutex (run)...");
                log.info(requests_lastsecond+" requests in the last second.");
                try {
                    mutex.acquire();
                    requestLimiter.release(requestsPerSecond);
                    requestsPerSecond = 0;
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
}
