package de.hpi.tdgt.atom

import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer
import de.hpi.tdgt.test.story.atom.Assignment
import de.hpi.tdgt.test.story.atom.Delay
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestAssignment {
    private var assignmentAtom: Assignment? = null

    @BeforeEach
    fun prepareTest() {
        assignmentAtom = Assignment()
        assignmentAtom!!.predecessorCount = 0
        assignmentAtom!!.repeat = 1
    }



    @Test
    fun copiesValue() {
        val assignments = HashMap<String, String>()
        assignments["val1"] = "val2"
        assignmentAtom!!.assignments = assignments

        val params = HashMap<String, String>()
        params["val1"] = "abc"
        //else we will not wait at all
            assignmentAtom!!.run(params)
        assertThat(assignmentAtom?.knownParams, Matchers.hasEntry("val2","abc"));
    }
    @Test
    fun insertsEmptyStringByDefault() {
        val assignments = HashMap<String, String>()
        assignments["val1"] = "val2"
        assignmentAtom!!.assignments = assignments

        val params = HashMap<String, String>()
        //else we will not wait at all
            assignmentAtom!!.run(params)
        assertThat(assignmentAtom?.knownParams, Matchers.hasEntry("val2",""));
    }

    @Test
    fun cloneCreatesEquivalentObject() = run {
        val clone = assignmentAtom!!.clone()
        assertThat("Clone should equal the cloned object!", clone == assignmentAtom)
    }

    @Test
    fun cloneCreatesOtherObject() = run {
        val clone = assignmentAtom!!.clone()
        Assertions.assertNotSame(clone, assignmentAtom)
    }
    @Test
    fun canBeDeserialized() {
        val test = Deserializer.deserialize(Utils().assignmentExample)
        assignmentAtom = test.getStories()[0].getAtoms()[0] as Assignment
        assertThat(assignmentAtom, Matchers.notNullValue())
        assertThat(assignmentAtom!!.assignments, Matchers.hasEntry("val1","val2"))
    }
}