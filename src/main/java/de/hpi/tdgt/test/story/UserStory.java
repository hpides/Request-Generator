package de.hpi.tdgt.test.story;

import de.hpi.tdgt.test.story.atom.Atom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.Arrays;
import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
public class UserStory implements Runnable, Cloneable{
    private double scalePercentage;
    private String name;
    private Atom[] atoms;

    public void setAtoms(Atom[] atoms){
        this.atoms = atoms;
        //set links
        Arrays.stream(atoms).forEach(atom -> atom.initSuccessors(this));
    }
    @Override
    public UserStory clone(){
        val story = new UserStory();
        story.setName(this.getName());
        story.setScalePercentage(this.getScalePercentage());
        story.atoms = new Atom[atoms.length];
        //make a clone for all local changes, e.g. predecessorsReady
        for(int i = 0; i < atoms.length; i++){
            story.getAtoms()[i] = atoms[i].clone();
        }
        //fix references
        Arrays.stream(story.getAtoms()).forEach(atom -> atom.initSuccessors(story));
        return story;
    }
    
    @Override
    public void run() {
        Thread storyThread = new Thread(() -> {
            try {
                UserStory clone;
                synchronized (this) {
                    clone = this.clone();
                }
                log.info("Running story "+clone.getName()+" in thread "+Thread.currentThread().getId());
                clone.getAtoms()[0].run(new HashMap<>());
            } catch (InterruptedException e) {
                log.error(e);
            }
        });
        storyThread.start();
        try {
            storyThread.join();
        } catch (InterruptedException e) {
            log.error(e);
        }

    }
}
