package de.hpi.tdgt

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.apache.commons.cli.*
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class Application {
    companion object {
        val requests = AtomicInteger(0);
        var repetitions = 0;
        var coroutines = 0;
        var host = "http://localhost"
        val client = Dsl.asyncHttpClient(DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(30000).setReadTimeout(30000))
        suspend fun sendRequest() {

            for (i in 1..repetitions) {
                val future = client.prepareGet(host).execute()
                future.toCompletableFuture().await()
                requests.incrementAndGet()
            }
        }


        suspend fun parallelRequests() = coroutineScope<Unit> {
            val jobs = LinkedList<Job>();
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
                val startTime = System.currentTimeMillis()
                //returns when requests are sent!
                runBlocking {
                    withContext(Dispatchers.IO) {
                        parallelRequests()
                    }
                }
                val endTime = System.currentTimeMillis();
                println("DONE: " + requests + " requests in " + (endTime - startTime) + " ms!")
            } catch (e: ParseException) {
                System.out.println(e.message)
                formatter.printHelp("utility-name", options)
                System.exit(1)
            }
        }
    }
}
