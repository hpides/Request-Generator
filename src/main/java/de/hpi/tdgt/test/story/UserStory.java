package de.hpi.tdgt.test.story;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.ThreadRecycler;
import de.hpi.tdgt.test.story.atom.Atom;
import de.hpi.tdgt.test.story.atom.WarmupEnd;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Getter
@Setter
@NoArgsConstructor
@Log4j2
public class UserStory implements Runnable, Cloneable{
    private double scalePercentage;
    private String name;
    private Atom[] atoms;
    @JsonIgnore
    @Getter
    private Test parent;
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
        story.setParent(parent);
        //fix references
        Arrays.stream(story.getAtoms()).forEach(atom -> atom.initSuccessors(story));
        return story;
    }
    
    @Override
    public void run() {
        Runnable storyRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    //get one of the tickets
                    if(Test.ActiveInstancesThrottler.getInstance() != null) {
                        try {
                            Test.ActiveInstancesThrottler.getInstance().allowInstanceToRun();
                        } catch (InterruptedException e) {
                            log.error("Interrupted wail waiting to be allowed to send a request, aborting: ",e);
                            return;
                        }
                    }
                    else {
                        log.warn("Internal error: Can not limit active story instances per second!");
                    }

                    UserStory clone;
                    synchronized (this) {
                        clone = UserStory.this.clone();
                    }
                    log.info("Running story " + clone.getName() + " in thread " + Thread.currentThread().getId());
                    try {
                        clone.getAtoms()[0].run(new HashMap<>());
                    } catch (ExecutionException e) {
                        log.error(e);
                    }
                    log.info("Finished story " + clone.getName() + " in thread " + Thread.currentThread().getId());
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        };
        try {
            ThreadRecycler.getInstance().getExecutorService().submit(storyRunnable).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        }
    }


    public boolean hasWarmup(){
        for(val atom : atoms){
            if(atom instanceof WarmupEnd){
                return true;
            }
        }
        return false;
    }

    /**
     * True if there are threads that run the activities of this story, false otherwise. Has to be set by Test.
     */
    @JsonIgnore
    private boolean isStarted;

    /**
     * Returns number of times a warmup in this story is waiting for a mutex.
     * @return int
     */
    public int numberOfWarmupEnds(){
        int warmupEnds = 0;
        for(val atom : atoms){
            if(atom instanceof WarmupEnd){
                warmupEnds+=atom.getRepeat();
            }
        }
        return warmupEnds;
    }
}
