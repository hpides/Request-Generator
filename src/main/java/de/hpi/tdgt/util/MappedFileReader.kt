package de.hpi.tdgt.util

import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture


class MappedFileReader//e.g. file not found//async reading taken from http://www.java2s.com/Tutorials/Java/Java_io/1050__Java_nio_Asynchronous.htm
(filepath: String) :Closeable {
    private lateinit var channel: AsynchronousFileChannel
    private lateinit var buffer: ByteBuffer
    private var mappedData:ByteArray = ByteArray(0)
    private var offset = 0
    private var closed = false

    var exception: Exception? = null
    private var readData = false
    private suspend fun readData() {
        try {
            //conversion taken from https://stackoverflow.com/a/28454743, handler from http://tutorials.jenkov.com/java-nio/asynchronousfilechannel.html
            val completableFuture = CompletableFuture<ByteBuffer>()

            channel.read(buffer, 0, buffer, object : CompletionHandler<Int, ByteBuffer> {
                override fun completed(result: Int, attachment: ByteBuffer) {
                    buffer.flip()
                    mappedData = ByteArray(buffer.limit())
                    buffer.get(mappedData)
                    buffer.clear()
                    completableFuture.complete(attachment)
                }

                override fun failed(exc: Throwable?, attachment: ByteBuffer?) {
                    completableFuture.completeExceptionally(exc)
                }
            })
            completableFuture.await()

            readData = true
        } catch (e:Exception){
            exception = e
        }
    }
    private val mutex = Semaphore(1)
    suspend fun nextLine(): String? {
        if(closed){
            return null
        }
        mutex.acquire()
        if (!readData) {
            readData()
        }
        val line = StringBuilder()
        if(offset >= mappedData.size){
            close()
            return line.toString()
        }
        var current = mappedData[offset].toChar()
        //left when line break is detected
        while (true) {
            if(offset + 1 >= mappedData.size){
                close()
                return line.toString()
            }
            val next = mappedData[offset+1].toChar()
            if (current == '\n' || current == '\r') {
                //Windows style linebreak, filter next character so it is not mistaken for the next line break
                if (next == '\n' && current == '\r') {
                    offset++
                }
                //clear line break for next run
                offset ++
                break
            } else {
                line.append(current)
            }
            offset++
            if(offset >= mappedData.size){
                close()
                return line.toString()
            }
            current = mappedData[offset].toChar()
        }
        mutex.release()
        return line.toString()
    }

    fun hasNextLine(): Boolean {
        if(closed) return false
        return  offset <= buffer.limit()
    }

    fun ioException(): Exception? {
        return exception
    }

    override fun close() {
        closed = true
        // not initialized if no file was found
        if(::channel.isInitialized) {
            channel.close()
        }
    }

    companion object {
        private val log = LogManager.getLogger(
                MappedFileReader::class.java
        )
    }

    init {
        try {
            val path = Paths.get(filepath)
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)
            buffer = ByteBuffer.allocate(channel.size().toInt())
            //e.g. file not found
        } catch (e:Exception){
            log.error("Error creating mappedFileReader: ",e)
            closed = true
        }
    }

}