package de.hpi.tdgt.util

import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.io.Closeable
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture


class MappedFileReader :Closeable {
    private val channel: AsynchronousFileChannel
    var buffer: ByteBuffer
    var mappedData:ByteArray = ByteArray(0)
    private var offset = 0
    private var closed = false
    internal class Attachment {
        var path: Path? = null
        var buffer: ByteBuffer? = null
        var asyncChannel: AsynchronousFileChannel? = null
    }

    //async reading taken from http://www.java2s.com/Tutorials/Java/Java_io/1050__Java_nio_Asynchronous.htm
    constructor(filepath: String) {
        val path = Paths.get(filepath)
        channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)
        buffer = ByteBuffer.allocate(channel.size().toInt())

        val attach = Attachment()
        attach.asyncChannel = channel
        attach.buffer = buffer
        attach.path = path
    }
    var exception: java.lang.Exception? = null
    var readData = false
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
        } catch (e:java.lang.Exception){
            exception = e
        }
    }
    val mutex = Semaphore(1)
    public suspend fun nextLine(): String? {
        if(closed){
            return null
        }
        mutex.acquire()
        if (!readData) {
            readData()
        }
        val line = StringBuilder()
        try {
            var current = mappedData[offset].toChar()
            //left when line break is detected
            while (true) {
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
                current = mappedData[offset].toChar()
            }
            //buffer is empty
        } catch (e: BufferUnderflowException) {
            close()
        }
        finally {
            mutex.release()
        }
        return line.toString()
    }

    fun hasNextLine(): Boolean {
        return offset <= buffer.limit() && !closed
    }

    fun ioException(): java.lang.Exception? {
        return exception
    }

    override fun close() {
        closed = true
        channel.close()
    }

}