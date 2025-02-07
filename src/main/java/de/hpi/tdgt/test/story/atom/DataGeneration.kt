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

package de.hpi.tdgt.test.story.atom

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hpi.tdgt.util.MappedFileReader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class DataGeneration : Atom() {
    var data: Array<String> = arrayOf()
    var table: String? = null

    var staticValues: Map<String,String> = HashMap()
    /**
     * Proportion of lines that should be skipped over in the target file --> used to split data on nodes
     */
    var offsetPercentage : Double = 0.0

    //should not be serialized or accessible from other classes
    @JsonIgnore
    private var sc: MappedFileReader? = null

    override suspend fun perform() {
        val generatedData = readBuffer()
        knownParams.putAll(staticValues)
        knownParams.putAll(generatedData)
    }

    public override fun performClone(): Atom {
        val ret = if(actuallyPerformClone){ DataGeneration() } else {this}
        ret.table = table
        ret.data = data
        return ret
    }

    //used for an error message for user
    @JsonIgnore
    var readLines = 0

    suspend fun readBuffer(): Map<String, String> {
        initScanner()
        //sc has a value now
        val buffer = HashMap<String, String>()
        //scanner is not thread save, but will only be assigned once. So we can synchronise on it.
//this sync prevents mixups when calling nextLine, e.g. two threads call it at the same time when only one Line remaining
        var line: String
        //sc might be null, if file not found
        if (sc != null) {
                if (sc!!.hasNextLine()) {
                    readLines++
                    val retrieved = sc!!.nextLine()
                    //two threads might end up in here at the exact time, but only one gets last line--> catch this
                    if(retrieved != null){
                        line = retrieved
                    }
                    else{
                        warnNoDataRemain()
                        return buffer
                    }
                    log.info("Retrieved " + line + "from table" + " in Thread " + Thread.currentThread().id + "for atom " + name)
                } else {
                    warnNoDataRemain()
                    return buffer
                }
                // Scanner suppresses exceptions
                if (sc!!.ioException() != null) {
                    log.error("Exception: ", sc!!.ioException())
                }
        } else {
            log.warn("Data generation $name could not read data from file $table.csv")
            return buffer
        }
        //can be done without synchronisation, saves time spent in sequential mode
        val values = line.split(";".toRegex()).toTypedArray()
        if (values.size < data.size - 1) {
            log.error("Generated data does not match required data!")
            reportFailureToUser(
                "Data Generation \"" + name + "\" has too few columns",
                data.size.toString() + " columns requested but only " + values.size + " found in file " + outputDirectory + "/" + table + ".csv"
            )
        } else {
            for (i in data.indices) { //assume last csv column was empty
                if (i >= values.size) {
                    buffer[data[data.size - 1]] = ""
                } else {
                    buffer[data[i]] = values[i]
                }
            }
        }
        return buffer
    }

    private fun warnNoDataRemain() {
        log.error("No data remains for atom $name")
        reportFailureToUser(
            "Data Generation \"$name\" has no data remaining",
                "read $readLines lines from file $outputDirectory/$table.csv"
        )
    }


    suspend fun initScanner() { //only one Thread is allowed to add a scanner at the same time; only need to synchronise scanner creation and retrieval
//stream might be null, if no file found
        if (sc == null) {
            synchronized(association) {
                if (association.containsKey(table)) {
                    sc = association[table]
                } else {
                    sc = MappedFileReader("$outputDirectory${File.separator}$table.csv")
                    association.put(table, sc)
                }
            }
        }
        if(offsetPercentage > 0){
            var lines = 0.0
            //since every line might have a different length, there is no way around reading the whole file once...
            while(sc!!.hasNextLine()){
                sc!!.nextLine()
                lines++
            }
            var target = kotlin.math.floor(offsetPercentage * lines).toLong()
            //to prevent repetition of this
            offsetPercentage = 0.0
            //we need a new scanner with reset offset
            sc!!.close()
            synchronized(association){
                association.remove(table)
            }
            sc = null
            //this re-initialises a fresh instance of sc; since offsetPercentage is 0.0, there will not be infinite recursion
            initScanner()
            //once this is done, the offset inside the scanner is where we want it
            //as a side effect, the next blocks are probably already prefetched by now
            for(i in 0L until target){
                sc?.nextLine()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is DataGeneration) return false
        val otherObject = other
        if (!otherObject.canEqual(this as Any)) return false
        if (!super.equals(otherObject)) return false
        if (!Arrays.deepEquals(data, otherObject.data)) return false
        val `this$table`: Any? = table
        val `other$table`: Any? = otherObject.table
        if (if (`this$table` == null) `other$table` != null else `this$table` != `other$table`) return false
        val `this$sc`: MappedFileReader? = this.sc
        val `other$sc`: MappedFileReader? = otherObject.sc
        if (if (`this$sc` == null) `other$sc` != null else `this$sc` != `other$sc`) return false
        return if (readLines != otherObject.readLines) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is DataGeneration
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        result = result * PRIME + Arrays.deepHashCode(data)
        val `$table`: Any? = table
        result = result * PRIME + (`$table`?.hashCode() ?: 43)
        val `$sc`: MappedFileReader? = this.sc
        result = result * PRIME + (`$sc`?.hashCode() ?: 43)
        result = result * PRIME + readLines
        return result
    }

    override val log: Logger
        get() = DataGeneration.log

    companion object {
        private val log = LogManager.getLogger(
            DataGeneration::class.java
        )
        //this is used to synchronise current line in all file(s)
        @JsonIgnore
        private val association: MutableMap<String?, MappedFileReader?> =
            HashMap()

        /**
         * Removes all class state.
         * Instances will start reading files from the beginning again.
         */
        @JvmStatic
        fun reset() { //close all Scanners
            for (scanner in association.values) {
                scanner!!.close()
            }
            association.clear()
        }

        @JvmField
        var outputDirectory = "."
    }

    /**
     * Only to be used by tests!
     */
    var actuallyPerformClone = true
}