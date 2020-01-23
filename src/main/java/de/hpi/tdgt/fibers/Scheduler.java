package de.hpi.tdgt.fibers;

import lombok.Getter;
import lombok.val;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class Scheduler {
    @Getter
    private static final Scheduler instance = new Scheduler();

    private final Fiber activeFiber = new Fiber();
    private Vector<Runnable> tasks = new Vector<>();
    public void yieldAndReschedule(){
        if(tasks.size() > 0) {
            val task = tasks.remove(0);
            activeFiber.switch_to(task);
        }
    }

    public void addTask(Runnable runnable){
        tasks.add(runnable);
    }
}
