package de.hpi.tdgt.test.story.atom

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.util.MappedFileReader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap

class Data_Generation : Atom() {
    var data: Array<String> = arrayOf()
    var table: String? = null

    var staticValues: Map<String,String> = HashMap()

    //should not be serialized or accessible from other classes
    @JsonIgnore
    private var sc: MappedFileReader? = null

    override suspend fun perform() {
        val generatedData = readBuffer()
        knownParams.putAll(staticValues)
        knownParams.putAll(generatedData)
    }

    public override fun performClone(): Atom {
        val ret = Data_Generation()
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
        log.error("No data remains for atom " + name)
        reportFailureToUser(
                "Data Generation \"" + name + "\" has no data remaining",
                "read $readLines lines from file $outputDirectory/$table.csv"
        )
    }


    private fun initScanner() { //only one Thread is allowed to add a scanner at the same time; only need to synchronise scanner creation and retrieval
//stream might be null, if no file found
        if (sc == null) {
            synchronized(association) {
                if (association.containsKey(table)) {
                    sc = association[table]
                } else {
                    sc = MappedFileReader("$outputDirectory/$table.csv")
                    association.put(table, sc)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Data_Generation) return false
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
        return other is Data_Generation
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

    companion object {
        private val log = LogManager.getLogger(
            Data_Generation::class.java
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
}