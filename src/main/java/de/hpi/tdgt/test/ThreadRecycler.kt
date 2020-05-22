package de.hpi.tdgt.test

import de.hpi.tdgt.util.PropertiesReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ThreadRecycler private constructor() {
    val executorService: ExecutorService
    //so it can be configured easily
    val THREADS_PER_CPU = PropertiesReader.getThreadsPerCPU()

    companion object {
        @JvmStatic
        var instance = ThreadRecycler()
            private set

        fun reset() {
            instance = ThreadRecycler()
        }

    }

    init {

        executorService = Executors.newUnboundedVirtualThreadExecutor()
    }
}