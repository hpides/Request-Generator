package de.hpi.tdgt.test.story.atom;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class Start extends Atom {
    @Override
    public void perform() throws InterruptedException {
        //Noop, just supposed to start the following atoms
    }

    @NotNull
    @Override
    protected Atom performClone() {
        //stateless
        return new Start();
    }
}
