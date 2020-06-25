package de.hpi.tdgt.test.story.atom

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.hpi.tdgt.test.ThreadRecycler
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.*
import org.apache.logging.log4j.Logger
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.regex.Pattern
import java.util.stream.Collectors


//ignore extra attributes that the frontend uses
@JsonIgnoreProperties(ignoreUnknown = true) //tell Jackson to use subclasses by type attribute
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
) //tell Jackson which subtypes are allowed
@JsonSubTypes(
    JsonSubTypes.Type(value = Request::class, name = "REQUEST"),
    JsonSubTypes.Type(value = DataGeneration::class, name = "DATA_GENERATION"),
    JsonSubTypes.Type(value = Delay::class, name = "DELAY"),
    JsonSubTypes.Type(value = Start::class, name = "START"),
    JsonSubTypes.Type(value = WarmupEnd::class, name = "WARMUP_END"),
    JsonSubTypes.Type(value = Assignment::class, name = "ASSIGNMENT")
)
abstract class Atom : Cloneable {
    var name: String? = null
    var id = 0
    var repeat = 0
    //should not have Getter, but needs Setter for Jackson
    private var successors: IntArray = IntArray(0)
    @JsonIgnore
    var predecessorCount = 0
    @JsonIgnore
    var predecessorsReady = 0
    @JsonIgnore
    val knownParams: MutableMap<String, String> =
        TreeMap()
    //use this method to get successors of this atom
    //do not try to read this from json, no accessors --> only used internally
    @JsonIgnore //should only be set by tests
    var successorLinks = arrayOf<Atom>()
        private set
    //e.g. for request, so it can account time to a story
    @JsonIgnore
    private var parent: UserStory? = null

    /**
     * During cloning, replacement will not work. This Flag disables it so the user does not get confised with warning messages.
     */
    protected var cloning = false;

    abstract suspend fun perform()

    protected fun reportFailureToUser(assertionName: String, message: String?, countAsFailedAssertion: Boolean =true) {
        var testId: Long = 0
        if (getParent() != null && getParent()!!.parent != null) {
            testId = getParent()!!.parent!!.testId
        }
        AssertionStorage.instance.addFailure(assertionName, message!!, testId, countAsFailedAssertion)
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    suspend fun run(dataMap: Map<String, String>?) {
        log.info("Running Atom $name in Thread ${Thread.currentThread().id}")
        //do not run if the corresponding test was aborted
        if(parent?.parent?.isAborted?.get() == true){
            log.info("Atom $name not running since parent test was aborted!")
            return
        }
        predecessorsReady += 1
        knownParams.putAll(dataMap!!)
        if (predecessorsReady >= predecessorCount) { //perform as often as requested
            for (i in 0 until repeat) {
                try {
                    perform()
                } catch(e:Exception){
                    log.error("Error running atom $name",e)
                }
            }
            runSuccessors()
        }
    }

    fun incrementPredecessorCount() {
        predecessorCount++
    }

    private fun getSuccessorIndex(successorID: Int, atoms: Array<Atom>): Int {
        for (i in atoms.indices) {
            if (atoms[i].id == successorID) {
                return i
            }
        }
        return -1
    }

    fun initSuccessors(parent: UserStory) {
        val successorList = Vector<Atom>()
        Arrays.stream(successors).forEach( { successor: Int ->
            val successorIndex = getSuccessorIndex(successor, parent.getAtoms())
            if (successorIndex == -1) {
                log.error("Could not find successor with id $successor for atom \"$name\"")
                return@forEach
            }
            successorList.add(parent.getAtoms()[successorIndex])
            parent.getAtoms()[successorIndex].incrementPredecessorCount()
        })
        //boilerplate
        this.successorLinks = successorList.toTypedArray()
        this.parent = parent
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private suspend fun runSuccessors() {
        if(!PropertiesReader.AsyncIO()) {
            val threads = Arrays.stream(this.successorLinks).map({ successorLink: Atom ->
                Runnable {
                    runBlocking {
                        runSuccessor(successorLink)
                    }
                }
            }).collect(Collectors.toUnmodifiableList());
            val futures = threads.stream().map({ runnable -> ThreadRecycler.instance.executorService.submit(runnable) }).collect(Collectors.toList());
            for (thread in futures) {
                if (!thread.isCancelled()) {
                    thread.get();
                }
            };
        }else{
            val jobs = Vector<Deferred<Unit>>()
            //withContext(Dispatchers.IO) {
                for (successorLink in successorLinks) {
                    jobs.add(GlobalScope.async { runSuccessor(successorLink) })
                }
            //}
            for(job in jobs){
                job.join()
            }
        }
    }

    private suspend fun runSuccessor(successorLink: Atom) {
        try {
            val clonedMap: HashMap<String, String> =
                    HashMap(this@Atom.knownParams)
            try {
                successorLink.run(clonedMap)
            } catch (e: ExecutionException) {
                log.error(e)
            }
        } catch (e: InterruptedException) {
            log.error(e)
        }
    }

    /**
     * This is used to make sure that not 2 copies are created of an atom with 2 predecessors. See Test.
     * @return
     */
    protected abstract fun performClone(): Atom

    /**
     * My original thought was to clone the atom of every story per Thread, while every Thread represents a user. This should guarantee that every user has an own state (knownParams, predecessorsReady, ...).
     * Using ThreadLocalStorage we should be able to avoid this problem, and keep our structure reentrant.
     * @return
     */
    public override fun clone(): Atom {
        val atom = performClone()
        //do NOT clone predecessorsReady or knownParams
        atom.id = id
        atom.name = name
        atom.repeat = repeat
        atom.parent = parent
        atom.successors = successors
        return atom
    }

    fun setSuccessors(successors: IntArray) {
        this.successors = successors
    }

    fun setSuccessorLinks(successorLinks: Array<Atom>) {
        this.successorLinks = successorLinks
    }

    fun setParent(parent: UserStory?) {
        this.parent = parent
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Atom) return false
        val otherObject = other
        if (!otherObject.canEqual(this as Any)) return false
        val `this$name`: Any? = name
        val `other$name`: Any? = otherObject.name
        if (if (`this$name` == null) `other$name` != null else `this$name` != `other$name`) return false
        if (id != otherObject.id) return false
        if (repeat != otherObject.repeat) return false
        if (!Arrays.equals(this.successorLinks, otherObject.successorLinks)) return false
        if (predecessorCount != otherObject.predecessorCount) return false
        if (predecessorsReady != otherObject.predecessorsReady) return false
        val `this$knownParams`: Any = knownParams
        val `other$knownParams`: Any = otherObject.knownParams
        if (`this$knownParams` != `other$knownParams`) return false
        return if (!Arrays.deepEquals(this.successorLinks, otherObject.successorLinks)) false else true
    }

    protected open fun canEqual(other: Any?): Boolean {
        return other is Atom
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = 1
        val `$name`: Any? = name
        result = result * PRIME + (`$name`?.hashCode() ?: 43)
        result = result * PRIME + id
        result = result * PRIME + repeat
        result = result * PRIME + Arrays.hashCode(this.successorLinks)
        result = result * PRIME + predecessorCount
        result = result * PRIME + predecessorsReady
        val `$knownParams`: Any = knownParams
        result = result * PRIME + `$knownParams`.hashCode()
        result = result * PRIME + Arrays.deepHashCode(this.successorLinks)
        return result
    }

    protected fun getParent(): UserStory? {
        return parent
    }

    public fun replaceWithKnownParams(toReplace: String, enquoteInsertedValue: Boolean, sanitizeXPATH: Boolean = false, sanitizeJSONPATH: Boolean = false): String? {
        var current = toReplace
        for ((key, value) in knownParams) {
            var useValue=value
            if(sanitizeXPATH){
                useValue = Request.sanitizeXPATH(value)
            }
            //sanitizeXPATH takes care of quotes
            if(enquoteInsertedValue && !sanitizeXPATH && !sanitizeJSONPATH) {
                // quotes in JSON need to be replaced
                useValue = useValue.replace("\"","\\\"")
                current = current.replace("$$key", '\"' + useValue + '\"')
            }
            else if(sanitizeJSONPATH){
                //inside a JSONPATH regex, forward slashes need to be escaped since they denominate regexes. Else they should not be replaced because it might alter the meaning.
                var isInRegex = false
                val keyStart = current.indexOf(key)
                if(keyStart > 0){
                    var index = 0;
                    while(index < keyStart){
                        if(current[index] == '/') isInRegex = !isInRegex
                        index++
                    }
                }
                if(isInRegex) {
                    useValue = useValue.replace("/","\\/")
                }
                //symbols that always need replacement
                useValue = useValue.replace("(","\\(").replace(")","\\)").replace("'","\\'").replace("\"","\\\"")

                current = current.replace("$$key", useValue)
            }
            else{
                current = current.replace("$$key", useValue)
            }
        }
        //should show a warning
        //need to consider surrounding characters in both direction, else it does not match...
        //method might be called during cloning (usage in setters). In this case, we do not want it to report failed assertions
        if (Pattern.matches(".*"+"\\"+"\$"+"[a-zA-Z0-9]+.*", current) && !cloning) {
            val p = Pattern.compile("\\$[a-zA-Z0-9]*")
            val m = p.matcher(current)
            val allUncompiled = HashSet<String>()
            while (m.find()) {
                allUncompiled.add(m.group())
            }
            val builder = StringBuilder()
            var first = true
            for (unmatched in allUncompiled) {
                if (!first) {
                    builder.append(',')
                }
                first = false
                //we want to show the "pure" variable names
                builder.append(' ').append(unmatched.replace("\$",""))
            }
            log.warn("Request $name: Could not replace variable(s) $builder")
            reportFailureToUser("Request $name: Could not replace variable(s) $builder", current)
        }
        return current
    }

    protected abstract val log : Logger


}