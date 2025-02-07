/*
 * WALT - A realistic load generator for web applications.
 *
 * Copyright 2020 Eric Ackermann <eric.ackermann@student.hpi.de>, Hendrik Bomhardt
 * <hendrik.bomhardt@student.hpi.de>, Benito Buchheim
 * <benito.buchheim@student.hpi.de>, Juergen Schlossbauer
 * <juergen.schlossbauer@student.hpi.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hpi.tdgt.util

import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture


class MappedFileReader//e.g. file not found//async reading taken from http://www.java2s.com/Tutorials/Java/Java_io/1050__Java_nio_Asynchronous.htm
(filepath: String) :Closeable {
    private lateinit var channel: AsynchronousFileChannel
    private lateinit var buffer: ByteBuffer
    private var mappedData:ByteArray = ByteArray(0)
    private var offset = 0L
    private var closed = false

    var exception: Exception? = null
    private var readData = false
    private suspend fun readData() {
        try {
            //conversion taken from https://stackoverflow.com/a/28454743, handler from http://tutorials.jenkov.com/java-nio/asynchronousfilechannel.html
            val completableFuture = CompletableFuture<ByteBuffer>()

            channel.read(buffer, offset, buffer, object : CompletionHandler<Int, ByteBuffer> {
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
    private var currentBufferOffset = 0
    suspend fun nextLine(): String? {

        if(closed){
            return null
        }
        mutex.acquire()
        if (!readData) {
            readData()
        }
        //method is synchronized
        val line = StringBuilder()
        if(offset >= fileSize){
            close()
            return line.toString()
        }
        var current = mappedData[currentBufferOffset].toChar()
        //left when line break is detected
        while (true) {
            if(offset + 1 >= fileSize){
                close()
                return line.toString()
            }
            val next = mappedData[currentBufferOffset+1].toChar()
            if (current == '\n' || current == '\r') {
                //Windows style linebreak, filter next character so it is not mistaken for the next line break
                if (next == '\n' && current == '\r') {
                    offset++
                    advanceBuffer()
                }
                //clear line break for next run
                offset ++
                advanceBuffer()
                break
            } else {
                //IntelliJ warned the append method with a char might be blocking
                line.append(current.toString())
            }
            offset++
            advanceBuffer()
            if(offset >= fileSize){
                close()
                return line.toString()
            }
            current = mappedData[currentBufferOffset].toChar()
        }
        mutex.release()
        return line.toString()
    }

    private suspend fun advanceBuffer() {
        currentBufferOffset++
        //passed mapped file segement --> map next segment
        if(currentBufferOffset == bufferSize - 1){
            currentBufferOffset = 0
            buffer.clear();
            readData()
        }
    }

    fun hasNextLine(): Boolean {
        if(closed) return false
        return  offset <= fileSize
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
        if(::buffer.isInitialized) {
            buffer.clear()
        }
    }
    //make sure the resources are freed
    protected fun finalize() { 
    if(!closed){
        log.error("Always close MappedFileReader instances, do not rely on the finalizer!");
    } 
    close();
    }
    companion object {
        private val log = LogManager.getLogger(
                MappedFileReader::class.java
        )
        //1 mb is almost guaranteed to be available, yet large enough to make sense (almost certainly multiple of system block size)
        @JvmStatic
        public var bufferSize:Int = 1024*1024*64
    }

    private var fileSize:Long = 0
    
    init {
        try {
            val path = Paths.get(filepath)
            fileSize = Files.size(path)
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)
            buffer = ByteBuffer.allocate(bufferSize)
            //e.g. file not found
        } catch (e:Exception){
            log.error("Error creating mappedFileReader: ",e)
            closed = true
        }
    }

}
