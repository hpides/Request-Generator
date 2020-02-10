package de.hpi.tdgt

import io.netty.handler.codec.http.HttpResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.cli.*
import org.asynchttpclient.Dsl
import org.asynchttpclient.Response
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class Application {
    companion object {
        val requests = AtomicInteger(0);
        val errorCodes = AtomicInteger(0);
        var repetitions = 0;
        var coroutines = 0;
        var host = "http://localhost"
        var client = Dsl.asyncHttpClient()
        var permits = 10000
        var requestLimiter = Semaphore(permits)

        suspend fun sendRequest() {
            for (i in 1..repetitions) {
                var response: Response? = null
                requestLimiter.withPermit {
                    val future = client.prepareGet(host).execute()
                     response = future.toCompletableFuture().await()
                }
                if(response!!.statusCode!=200){
                    errorCodes.incrementAndGet()
                }
                requests.incrementAndGet()
            }
        }


        suspend fun parallelRequests() = coroutineScope {
            val jobs = LinkedList<Job>();
            requestLimiter = Semaphore(permits)
            println("Using $permits permits!");
            for (i in 1..coroutines) {
                val job = async { sendRequest() }
                jobs.add(job)
            }
            for (job in jobs) {
                job.join()
            }
        }

        @ObsoleteCoroutinesApi
        @JvmStatic fun main(args: Array<String>) {
            val options = Options()
            val coroutines_option = Option("c", "coroutines", true, "coroutines to use")
            coroutines_option.isRequired = true
            options.addOption(coroutines_option)

            val host_option = Option("h", "host", true, "host to ping")
            host_option.isRequired = false
            options.addOption(host_option)
            val repeat = Option("r", "repeat", true, "requests per coroutine")
            repeat.isRequired = true
            options.addOption(repeat)

            val concurrent_requests = Option("p", "parallel_requests", true, "open requests in parallel")
            concurrent_requests.isRequired = false
            options.addOption(concurrent_requests)

            val parser = DefaultParser()
            val formatter = HelpFormatter()
            val cmd: CommandLine
            try {
                cmd = parser.parse(options, args)

                repetitions = cmd.getOptionValue("repeat").toInt()
                coroutines = cmd.getOptionValue("coroutines").toInt()
                if(cmd.hasOption("host")){
                    host = cmd.getOptionValue("host")
                }
                if(cmd.hasOption("parallel_requests")){
                    permits = cmd.getOptionValue("parallel_requests").toInt()
                }
                val startTime = System.currentTimeMillis()
                //returns when requests are sent!
                runBlocking {
                    withContext(Dispatchers.IO) {
                        parallelRequests()
                    }
                }
                val endTime = System.currentTimeMillis();
                println("DONE: " + requests + " requests in " + (endTime - startTime) + " ms with "+ errorCodes.get()+" errors!")
                client.close()
            } catch (e: ParseException) {
                System.out.println(e.message)
                formatter.printHelp("utility-name", options)
                System.exit(1)
            }
        }
    }
}
