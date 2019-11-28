package de.hpi.tdgt.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Vector;
import java.util.concurrent.Semaphore;

import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;
import lombok.val;

@Getter
@Setter
@NoArgsConstructor
public class Test {
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;
    private int requests_per_second;
    public void start() throws InterruptedException {
        //this thread makes sure that requests per second get limited
        RequestThrottler.setInstance(this.requests_per_second);
        Thread watchdog = new Thread(RequestThrottler.getInstance());
        watchdog.setPriority(Thread.MAX_PRIORITY);
        watchdog.start();
        val threads = new Vector<Thread>();
        for(int i=0; i < stories.length; i++){
            //repeat stories as often as wished
            for(int j = 0; j < scaleFactor * stories[i].getScalePercentage(); j++) {
                val thread = new Thread(stories[i]);
                thread.start();
                threads.add(thread);
            }
        }
        for(val thread : threads){
            thread.join();
        }
        watchdog.interrupt();
        //remove global state
        RequestThrottler.reset();
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
        int requestsPerSecond = 0;
        private static void reset(){
            instance = null;
        }
        /**
         * Stops threads from increasing requests per second while re-creating tickets
         */
        private final Semaphore mutex = new Semaphore(1,true);
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
