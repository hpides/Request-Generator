package de.hpi.tdgt.test;

import de.hpi.tdgt.util.PropertiesReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
@Log4j2
public class ThreadRecycler {
    @Getter
    private static ThreadRecycler instance = new ThreadRecycler();

    public static void reset(){
        instance = new ThreadRecycler();
    }
    @Getter
    private ExecutorService executorService;
    //so it can be configured easily
    public final Integer THREADS_PER_CPU=PropertiesReader.getThreadsPerCPU();
    private ThreadRecycler(){
        int cpus = Runtime.getRuntime().availableProcessors();
        //I/O-heavy program, so threads wait a lot, and we can use more threads that we have CPUs
        executorService = new UpfrontCreationExecutorService(cpus * THREADS_PER_CPU);
    }

    private class UpfrontCreationExecutorService implements ExecutorService{
        int threads;
        Thread[] workers;
        ThreadRunnable[] runnables;

        int lowestUnusedThread;
        public UpfrontCreationExecutorService(int threads){
            log.info("Creating "+threads+" threads!");
            init(threads);
        }

        private void init(int threads) {
            synchronized (this) {
                lowestUnusedThread = 0;
                this.threads = threads;
                workers = new Thread[threads];
                runnables = new ThreadRunnable[threads];
                for (int i = 0; i < threads; i++) {
                    runnables[i] = new ThreadRunnable();
                    workers[i] = new Thread(runnables[i]);
                    workers[i].start();
                }
            }
        }

        @AllArgsConstructor
        private class ThreadFuture implements Future{

            private Thread thread;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if(!thread.isAlive())
                return false;
                thread.interrupt();
                return true;
            }

            @Override
            public boolean isCancelled() {
                return thread.isInterrupted();
            }

            @Override
            public boolean isDone() {
                return !thread.isAlive();
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                thread.join();
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                thread.join();
                return null;
            }
        }

        private class ThreadRunnable implements Runnable{
            @Setter
            private Runnable actualRunnable = null;
            @Getter
            private Semaphore toggle = new Semaphore(0);
            @Override
            public void run() {
                try {
                    toggle.acquire();
                } catch (InterruptedException e) {
                    return;
                }
                actualRunnable.run();
            }
        }

        @Override
        public void shutdown() {
            Arrays.stream(workers).forEach(Thread::interrupt);
            init(threads);
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            ThreadRunnable runnable;
            Thread thread;
            synchronized (this){
                runnable = runnables[lowestUnusedThread];
                thread = workers[lowestUnusedThread];
                lowestUnusedThread ++;
            }
            runnable.setActualRunnable(task);
            runnable.getToggle().release(1);
            return new ThreadFuture(thread);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public void execute(Runnable command) {

        }
    }
}
