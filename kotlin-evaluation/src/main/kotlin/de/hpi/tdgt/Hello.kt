package de.hpi.tdgt

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.apache.commons.cli.*
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger


class Application {
    companion object {
        val requests = AtomicInteger(0);
        var repetitions = 0;
        var coroutines = 0;
        var host = "http://localhost"
        var requestConfig:RequestConfig? = RequestConfig.custom()
        .setSocketTimeout(300000)
        .setConnectTimeout(300000).build()
        val client = HttpAsyncClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setMaxConnPerRoute(1000000)
            .setMaxConnTotal(100000)
            .build()

        fun HttpAsyncClient.execute(request: HttpUriRequest): CompletableFuture<HttpResponse> {
            val future = CompletableFuture<HttpResponse>()

            this.execute(request, object : FutureCallback<HttpResponse> {
                override fun completed(result: HttpResponse) {
                    future.complete(result)
                }

                override fun cancelled() {
                    future.cancel(false)
                }

                override fun failed(ex: Exception) {
                    future.completeExceptionally(ex)
                }
            })

            return future
        }

        suspend fun sendRequest() {
            for (i in 1..repetitions) {
                val request = HttpGet(host)
                val future = client.execute(request)
                val response = future.toCompletableFuture().await()
                //println(response.statusLine)
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
                client.start()
                val startTime = System.currentTimeMillis()
                //returns when requests are sent!
                runBlocking {
                    withContext(Dispatchers.IO) {
                        parallelRequests()
                    }
                }
                val endTime = System.currentTimeMillis();
                println("DONE: " + requests + " requests in " + (endTime - startTime) + " ms!")
                client.close()
            } catch (e: ParseException) {
                System.out.println(e.message)
                formatter.printHelp("utility-name", options)
                System.exit(1)
            }
        }
    }
}
