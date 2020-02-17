package de.hpi.tdgt.test.story.atom;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.Semaphore;

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
public class WarmupEnd extends Atom {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WarmupEnd.class);

    public static int getWaiting() {
        return WarmupEnd.waiting;
    }

    @Override
    public void perform() throws InterruptedException {
        addWaiter();
        warmupEnd.acquire();
    }

    @Override
    protected Atom performClone() {
        return new WarmupEnd();
    }

    //Test knows how many warmupEnds should be waiting for this semaphore.
    //So when this number is reached, it can release all of them and thus continue processing.
    private static final Semaphore warmupEnd = new Semaphore(0);
    private static int waiting = 0;
    private static synchronized void addWaiter(){
        log.info("Added a waiter to the existing "+waiting+" waiters!");
        waiting ++;
    }

    /**
     * Let all waiting warmup ends continue.
     */
    public static void startTest(){
        warmupEnd.release(waiting);
        waiting = 0;
    }
}
