package de.hpi.tdgt.test.story.atom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
//tell Jackson to use subclasses by type attribute
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
//tell Jackson which subtypes are allowed
@JsonSubTypes({
        @JsonSubTypes.Type(value = Request.class, name = "REQUEST"),
        @JsonSubTypes.Type(value = Data_Generation.class, name = "DATA_GENERATION"),
        @JsonSubTypes.Type(value = Delay.class, name = "DELAY"),
        @JsonSubTypes.Type(value = Start.class, name = "START"),
})
@EqualsAndHashCode
@Log4j2
public abstract class Atom implements Cloneable {
    private String name;
    private int id;
    private int repeat;

    //should not have Getter, but needs Setter for Jackson
    @Getter(AccessLevel.NONE)
    private int[] successors;

    @JsonIgnore
    private int predecessorCount = 0;
    @JsonIgnore
    private int predecessorsReady;

    @JsonIgnore
    private Map<String, String> knownParams = new HashMap<>();

    //do not try to read this from json, no accessors --> only used internally
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Atom[] successorLinks = new Atom[0];

    public abstract void perform() throws InterruptedException;
    
    //use this method to get successors of this atom
    public Atom[] getSuccessors() {
        return successorLinks;
    }

    public void run(Map<String,String> dataMap) throws InterruptedException {
        log.info("Running Atom "+getName()+" in Thread "+Thread.currentThread().getId());
        this.setPredecessorsReady(this.getPredecessorsReady() + 1);
        getKnownParams().putAll(dataMap);
        if (this.getPredecessorsReady() >= predecessorCount) {
            //perform as often as requested
            for(int i = 0; i < repeat; i++) {
                perform();
            }
            runSuccessors();
        }
    }

    public void incrementPredecessorCount() {
        predecessorCount++;
    }

    public void initSuccessors(UserStory parent) {
        val successorList = new Vector<Atom>();
        Arrays.stream(successors).forEach(successor -> {
            successorList.add(parent.getAtoms()[successor]);
            parent.getAtoms()[successor].incrementPredecessorCount();
        });
        //boilerplate
        this.successorLinks = successorList.toArray(new Atom[0]);
    }

    private void runSuccessors() throws InterruptedException {
        val threads = Arrays.stream(successorLinks).map(successorLink -> new Thread( () -> {
            try {
                val clonedMap = new HashMap<String, String>(this.getKnownParams());
                successorLink.run(clonedMap);
            } catch (InterruptedException e) {
                log.error(e);
            }
        })).collect(Collectors.toUnmodifiableList());
        threads.forEach(Thread::start);
        for(val thread : threads){
            thread.join();
        }
    }

    /**
     * This is used to make sure that not 2 copies are created of an atom with 2 predecessors. See Test.
     * @return
     */
    protected abstract Atom performClone();
    private boolean alreadyCloned = false;
    /**
     * My original thought was to clone the atom of every story per Thread, while every Thread represents a user. This should guarantee that every user has an own state (knownParams, predecessorsReady, ...).
     * Using ThreadLocalStorage we should be able to avoid this problem, and keep our structure reentrant.
     * @return
     */
    @Override
    public Atom clone(){
        val atom = performClone();
        //do NOT clone predecessorsReady or knownParams
        atom.setId(this.getId());
        atom.setName(this.getName());
        atom.setRepeat(this.getRepeat());
        atom.successors = successors;
        return atom;
    }
}
