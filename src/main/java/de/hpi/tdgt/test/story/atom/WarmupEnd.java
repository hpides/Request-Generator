package de.hpi.tdgt.test.story.atom;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
@Log4j2
public class WarmupEnd extends Atom {
    @Override
    public void perform() throws InterruptedException {
        addWaiter();
        warmupEnd.acquire();
        try {
            runSuccessors();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        }
    }

    @Override
    protected Atom performClone() {
        return new WarmupEnd();
    }

    //Test knows how many warmupEnds should be waiting for this semaphore.
    //So when this number is reached, it can release all of them and thus continue processing.
    private static final Semaphore warmupEnd = new Semaphore(0);
    @Getter
    @Setter(AccessLevel.NONE)
    private static int waiting = 0;
    private static synchronized void addWaiter(){
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
