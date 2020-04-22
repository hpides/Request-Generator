package de.hpi.tdgt.atom

import de.hpi.tdgt.Utils
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.Data_GenerationAtom
import de.hpi.tdgt.test.story.atom.Data_GenerationAtom.Companion.reset
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

class TestDataGeneration {
    private var firstGenerationAtom: Data_GenerationAtom? = null
    private var secondGenerationAtom: Data_GenerationAtom? = null
    private var thirdGenerationAtom: Data_GenerationAtom? = null
    private var users: File? = null
    private var posts: File? = null
    @BeforeEach
    @Throws(IOException::class)
    fun beforeEach() {
        firstGenerationAtom = Data_GenerationAtom()
        firstGenerationAtom!!.data = arrayOf("username", "password")
        firstGenerationAtom!!.table = "users"
        firstGenerationAtom!!.repeat = 1
        secondGenerationAtom = Data_GenerationAtom()
        secondGenerationAtom!!.data = arrayOf("username", "password")
        secondGenerationAtom!!.table = "users"
        secondGenerationAtom!!.repeat = 1
        thirdGenerationAtom = Data_GenerationAtom()
        thirdGenerationAtom!!.data = arrayOf("title", "text")
        thirdGenerationAtom!!.table = "posts"
        thirdGenerationAtom!!.repeat = 1
        users = File("users.csv")
        users!!.deleteOnExit()
        var os = FileOutputStream(users)
        IOUtils.copy(Utils().usersCSV, os)
        os.close()
        posts = File("posts.csv")
        posts!!.deleteOnExit()
        os = FileOutputStream(posts)
        IOUtils.copy(Utils().postsCSV, os)
        os.close()
    }

    @AfterEach
    fun afterEach() { //clear side effects
        reset()
        log.info("Deleted users: " + users!!.delete())
        log.info("Deleted posts: " + posts!!.delete())
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun firstGenerationShouldContainFirstElementOfUsersCSV() {
        var params: Map<String, String> = HashMap()
        runBlocking{firstGenerationAtom!!.run(params)}
        params = firstGenerationAtom!!.knownParams
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("username", "AMAR.Aaccf")
        )
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("password", "Dsa9h")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun firstGenerationShouldContainSecondElementOfUsersCSV() {
        var params: Map<String, String> = HashMap()
        runBlocking{firstGenerationAtom!!.run(params)}
        runBlocking{firstGenerationAtom!!.run(params)}
        params = firstGenerationAtom!!.knownParams
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("username", "ATP.Aaren")
        )
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("password", "uwi4tQngkLL")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun differentGenerationsProduceDifferentData() {
        var params: Map<String, String> = HashMap()
        var otherParams: Map<String, String> =
            HashMap()
        runBlocking{firstGenerationAtom!!.run(params)}
        params = firstGenerationAtom!!.knownParams
        //pointer to thread-local storage that will be overwritten immediately
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("username", "AMAR.Aaccf")
        )
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("password", "Dsa9h")
        )
        runBlocking{secondGenerationAtom!!.run(params)}
        otherParams = secondGenerationAtom!!.knownParams
        MatcherAssert.assertThat<Map<String, String>>(
            otherParams,
            Matchers.hasEntry("username", "ATP.Aaren")
        )
        MatcherAssert.assertThat<Map<String, String>>(
            otherParams,
            Matchers.hasEntry("password", "uwi4tQngkLL")
        )
    }

    private inner class DataGenRunnable : Runnable {
        private var gen: Data_GenerationAtom? = null
        //make sure to access state in the thread the operation ran in
        var knownParams: Map<String, String>? = null
            private set

        override fun run() {
            try {
                runBlocking {
                    gen!!.run(HashMap())
                }
            } catch (e: InterruptedException) {
                log.error(e)
            } catch (e: ExecutionException) {
                log.error(e)
            }
            knownParams = gen!!.knownParams
        }

        fun setGen(gen: Data_GenerationAtom?) {
            this.gen = gen
        }
    }

    private fun runAsync(generationAtom: Data_GenerationAtom?): DataGenRunnable {
        val ret = DataGenRunnable()
        ret.setGen(generationAtom)
        return ret
    }

    @Test
    @Throws(InterruptedException::class)
    fun differentGenerationsWithSameTableProduceDifferentDataInDifferentThreads() {
        val params: Map<String, String>
        val otherParams: Map<String, String>
        val generationRunnable = runAsync(firstGenerationAtom)
        val otherGenerationRunnable = runAsync(secondGenerationAtom)
        val generationThread = Thread(generationRunnable)
        generationThread.start()
        val otherGenerationThread = Thread(otherGenerationRunnable)
        otherGenerationThread.start()
        generationThread.join()
        otherGenerationThread.join()
        params = generationRunnable.knownParams!!
        otherParams = otherGenerationRunnable.knownParams!!
        val allValues = ArrayList(params.values)
        allValues.addAll(otherParams.values)
        //We do not know in what sequence the Threads did run.
//But one thread should have read the first line, the other thread the other line
        MatcherAssert.assertThat(
            allValues,
            Matchers.containsInAnyOrder("AMAR.Aaccf", "Dsa9h", "ATP.Aaren", "uwi4tQngkLL")
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun differentGenerationsWithDifferentTablesProduceDifferentDataInDifferentThreads() {
        val params: Map<String, String>
        val otherParams: Map<String, String>
        val thirdParams: Map<String, String>
        val generationRunnable = runAsync(firstGenerationAtom)
        val otherGenerationRunnable = runAsync(secondGenerationAtom)
        val thirdGenerationRunnable = runAsync(thirdGenerationAtom)
        val generationThread = Thread(generationRunnable)
        generationThread.start()
        val otherGenerationThread = Thread(otherGenerationRunnable)
        otherGenerationThread.start()
        val thirdGenerationThread = Thread(thirdGenerationRunnable)
        thirdGenerationThread.start()
        thirdGenerationThread.join()
        generationThread.join()
        otherGenerationThread.join()
        params = generationRunnable.knownParams!!
        val allValues = ArrayList(params.values)
        otherParams = otherGenerationRunnable.knownParams!!
        allValues.addAll(otherParams.values)
        //We do not know in what sequence the Threads did run.
//But one thread should have read the first line, the other thread the other line
        MatcherAssert.assertThat(
            allValues,
            Matchers.containsInAnyOrder("AMAR.Aaccf", "Dsa9h", "ATP.Aaren", "uwi4tQngkLL")
        )
        thirdParams = thirdGenerationRunnable.knownParams!!
        MatcherAssert.assertThat(
            thirdParams,
            Matchers.hasEntry(
                "title",
                "young boy becomes the cause of some real soul searching, as their family circle"
            )
        )
        MatcherAssert.assertThat(
            thirdParams, Matchers.hasEntry(
                "text",
                "$9 isn't too heavy to carry my pack with good digital camera kodak sells theDC3200.I bought this tripod for video tapping an upcomming family wedding and have tried it hundred times on newspaper up to $200 for it as being good tripod for stabilization and one on the small control buttons on the handle. The panning operation is likewise acceptable. The telescopic locking legs work well, but are light weight make it happen. This makes titling snap. Bundled software is easy to handle.The LCD is clear and easy to connect and engage. Remote control functions and handle are easy enough to prevent 'shaky hand', and not to bump the tripod warmed up in the box. Being an older Sony camera which liked most. without the use of the VCT-D680RM's remote control features. The DSC-N2 doesn't have good job.I would recommend kodak accessories for any kind of maddening to the PDA via the SD card...guess that's the case with most tiny cameras, and this records in SP (60 min for regular tapes) and LP (90 min), so you'll want to use which liked most. without the use of the tape in the video. \"Stiction\" is the 8-bit color. There are two problems. Check out the specifications and you need to edit your recordings it will NOT work with Cannon cameras.I like the problem with doing this as thought it would.I would buy this one. Recording quality is pretty good with an add-on high capacity battery. Best of all, you can get it for $10. Really what use the cards with RAW files and curves. It is what it is...and does what it is...and does what it was working fine, but the external microphone to remove the noise, in which PIXELLA converts the movie to MPEG. Dazzle USB brings in my movies that don't already have firewire interface and even though I'm only using the 460x digital zoom is fantastic, but you can plug it into the handle. The panning operation is likewise acceptable. The telescopic locking legs work well, but are light weight and size considerations and the computer asks if you are concerned about the camera with more high-end tripods. The other models looked at included the Sony VCT1170RM ($365) and far from Canon GL-1s or Xl-1s, but for what it was as good as radio or television.2) Solution from yet another user:Buy generic external mic, or Canon's DM-50 Microphone. This completely eliminates the motor noise, which is my first copy from the handle is easily removable and can afford DV or Digital 8mm camcorder. Yet.Enter the TRV-108, nifty menu system navigated by little disappointed when opened the package three credit card size pieces of plastic. My guess, and may well be wrong, is that one worked fine.I have two original Canon batteries, and now will take more videos of my camera, but they are with my Mac is on newspaper up to average bumps and knocks.....especially attached to the subject. The Canon brand is worth the extra cost."
            )
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testClone() {
        val params: Map<String, String> = HashMap()
        val clone = firstGenerationAtom!!.clone()
        runBlocking{firstGenerationAtom!!.run(params)}
        MatcherAssert.assertThat<Map<String, String>>(
            clone.knownParams,
            Matchers.anEmptyMap()
        )
    }

    @Test
    fun cloneCreatesEquivalentObject() {
        val params: Map<String, String> = HashMap()
        val clone = firstGenerationAtom!!.clone()
        MatcherAssert.assertThat(clone, Matchers.equalTo<Atom?>(firstGenerationAtom))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun dataGenerationCanHandleEmptyValuesInLastCSVColumn() {
        for (i in 0..16) {
            runBlocking{firstGenerationAtom!!.run(HashMap())}
        }
        //17th line is "Abdul-Nour.Abdallah;"
        val params: Map<String, String>
        params = firstGenerationAtom!!.knownParams
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("username", "Abdul-Nour.Abdallah")
        )
        MatcherAssert.assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("password", "")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun dataGenerationThrowsNoErrorsIfEmpty() {
        val dataGeneration = Data_GenerationAtom()
        dataGeneration.data = arrayOf()
        dataGeneration.table = ""
        dataGeneration.predecessorCount = 0
        dataGeneration.repeat = 1
        runBlocking {
            dataGeneration.run(HashMap())
        }
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun dataGenerationCanGenerateStaticValues() {
        val dataGeneration = Data_GenerationAtom()
        dataGeneration.data = arrayOf()
        val valuesToGenerate:MutableMap<String,String> = HashMap<String, String>()
        valuesToGenerate["val1"] = "abc"
        valuesToGenerate["val2"] = "def"
        dataGeneration.staticValues = valuesToGenerate
        dataGeneration.table = ""
        dataGeneration.predecessorCount = 0
        dataGeneration.repeat = 1
        runBlocking {
            dataGeneration.run(HashMap())
        }
        assertThat(dataGeneration.knownParams, equalTo(valuesToGenerate))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun dataGenerationCanSeekToItsOffsetInAFile() {
        val mockTest = de.hpi.tdgt.test.Test()
        //file is 70 lines long, so this data generation should produce one line exactly (second line of the file)
        mockTest.nodes = 70
        mockTest.nodeNumber = 1
        mockTest.scaleFactor = 1
        mockTest.repeat = 1
        val mockStory = UserStory()
        mockTest.setStories(arrayOf(mockStory))
        mockStory.scalePercentage = 1.0
        mockStory.setAtoms(arrayOf(firstGenerationAtom!!))
        firstGenerationAtom!!.setParent(mockStory)
        //else knownParams would be gone
        firstGenerationAtom!!.actuallyPerformClone = false
        var params: Map<String, String> = HashMap()
        runBlocking{mockTest.start()}
        params = firstGenerationAtom!!.knownParams
        assertThat<Map<String, String>>(
            params,
            Matchers.hasEntry("username", "ATP.Aaren")
        )
    }

    companion object {
        private val log = LogManager.getLogger(
            TestDataGeneration::class.java
        )
    }
}