package de.hpi.tdgt

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import org.apache.commons.cli.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
class Application {
    companion object {
        val requests = AtomicInteger(0);
        var repetitions = 0;
        var coroutines = 0;
        var host = "http://localhost"
        val client = HttpClient(Apache);
        @KtorExperimentalAPI
        suspend fun sendRequest() {

            for (i in 1..repetitions) {
                client.get<String>(host)
                requests.incrementAndGet()
            }
        }

        @KtorExperimentalAPI
        suspend fun parallelRequests() = coroutineScope<Unit> {
            val jobs = LinkedList<Job>();
            for (i in 1..coroutines) {
                val job = async { sendRequest() }
                jobs.add(job)
            }
            for (job in jobs) {
                job.join()
            }
            client.close();
        }

        @ObsoleteCoroutinesApi
        @KtorExperimentalAPI
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
