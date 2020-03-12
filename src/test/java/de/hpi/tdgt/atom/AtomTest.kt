package de.hpi.tdgt.atom

import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.story.UserStory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class AtomTest {
    private var story: UserStory? = null
    @BeforeEach
    @Throws(IOException::class)
    fun prepare() {
        story = deserialize(Utils().exampleJSON).getStories()[0]
    }

    @Test
    fun testCloneCreatesNotTwoDifferentCopiesOfSameObject() {
        val atom = story!!.getAtoms()[0]
        val clone = atom.clone()
        var successor1 = atom
        while (successor1.name != "User löschen story 1") {
            successor1 = if (successor1.name == "User anlegen") {
                successor1.successorLinks[1]
            } else successor1.successorLinks[0]
        }
        var successor2 = atom
        while (successor2.name != "User löschen story 1") {
            successor2 = successor2.successorLinks[0]
        }
        Assertions.assertSame(successor1, successor2)
    }
}