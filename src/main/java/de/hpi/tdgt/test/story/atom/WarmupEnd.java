package de.hpi.tdgt.test.story.atom;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import co.paralleluniverse.strands.concurrent.Semaphore;

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
public class WarmupEnd extends Atom {
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
    private static final Semaphore mutex = new Semaphore(1);
    @Getter
    @Setter(AccessLevel.NONE)
    private static int waiting = 0;
    //synchronized not allowed in fiber
    private static void addWaiter() throws InterruptedException {
        mutex.acquire();
        waiting ++;
        mutex.release();
    }

    /**
     * Let all waiting warmup ends continue.
     */
    public static void startTest(){
        warmupEnd.release(waiting);
        waiting = 0;
    }
}
