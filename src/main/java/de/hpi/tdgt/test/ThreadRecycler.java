package de.hpi.tdgt.test;

import de.hpi.tdgt.util.PropertiesReader;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadRecycler {
    @Getter
    private static ThreadRecycler instance = new ThreadRecycler();

    public static void reset(){
        instance = new ThreadRecycler();
    }
    @Getter
    private ExecutorService executorService;
    //so it can be configured easily
    public final int THREADS_PER_CPU=PropertiesReader.getThreadsPerCPU();
    private ThreadRecycler(){
        int cpus = Runtime.getRuntime().availableProcessors();
        //I/O-heavy program, so threads wait a lot, and we can use more threads that we have CPUs
        executorService = Executors.newWorkStealingPool(cpus * THREADS_PER_CPU);
    }
}
