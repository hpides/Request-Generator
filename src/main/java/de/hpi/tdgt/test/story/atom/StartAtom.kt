package de.hpi.tdgt.test.story.atom

import lombok.EqualsAndHashCode

@EqualsAndHashCode(callSuper = false)
class StartAtom : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
        //Noop, just supposed to start the following atoms
    }

    override fun performClone(): Atom {
        //stateless
        return StartAtom()
    }
}