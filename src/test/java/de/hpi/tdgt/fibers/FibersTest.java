package de.hpi.tdgt.fibers;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FibersTest {

    private boolean called = false;
    private int count = 0;
    private int otherCount = 0;
    private Runnable run =  new Runnable() {
        @Override
        public void run() {
            called  = true;
        }
    };

    private Runnable counter =  new Runnable() {
        @Override
        public void run() {
            count++;
        }
    };

    private Runnable otherCounter =  new Runnable() {
        @Override
        public void run() {
            otherCount++;
        }
    };
    @Test
    public void fibersRunRunnableAndReturn(){
        Fiber f = new Fiber();
        System.out.println("Created fiber");
        f.switch_to(run);
        assertThat("called should have been toggled!", called);
    }

    @Test
    public void fibersRunRunnableMultipleTimes(){
        Fiber f = new Fiber();
        System.out.println("Created fiber");
        for(int i = 0; i < 10; i++) {
            f.switch_to(counter);
        }
        assertThat(count, equalTo(10));
    }

    @Test
    public void CanHandleMultipleFibers(){
        Fiber f = new Fiber();
        System.out.println("Created fiber");
        for(int i = 0; i < 10; i++) {
            f.switch_to(counter);
            f.switch_to(otherCounter);
        }
        assertThat(count, equalTo(otherCount));
    }
}
