package de.hpi.tdgt.test.story.atom;

public class Start extends Atom {
    @Override
    public void perform() throws InterruptedException {
        //Noop, just supposed to start the following atoms
    }

    @Override
    protected Atom performClone() {
        //stateless
        return new Start();
    }
}
