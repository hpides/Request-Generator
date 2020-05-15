package de.hpi.tdgt.test.story.atom

import lombok.EqualsAndHashCode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@EqualsAndHashCode(callSuper = false)
class Start : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
        //Noop, just supposed to start the following atoms
    }

    override fun performClone(): Atom {
        //stateless
        return Start()
    }

    override val log: Logger
        get() = Start.log

    companion object {
        val log = LogManager.getLogger(Start::class.java)
    }
}