package de.hpi.tdgt.test.story.atom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.hpi.tdgt.test.ThreadRecycler;
import lombok.*;
import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
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
        @JsonSubTypes.Type(value = WarmupEnd.class, name = "WARMUP_END"),
})
@EqualsAndHashCode(exclude = {"parent"})
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
    private final Map<String, String> knownParams = new HashMap<>();

    //do not try to read this from json, no accessors --> only used internally
    @JsonIgnore
    //should only be set by tests
    @Getter(AccessLevel.NONE)
    private Atom[] successorLinks = new Atom[0];
    //e.g. for request, so it can account time to a story
    @JsonIgnore
    @Getter(AccessLevel.PROTECTED)
    private UserStory parent;
    public abstract void perform() throws InterruptedException;
    
    //use this method to get successors of this atom
    public Atom[] getSuccessors() {
        return successorLinks;
    }

    public void run(Map<String,String> dataMap) throws InterruptedException, ExecutionException {
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

    private int getSuccessorIndex(int successorID, Atom[] atoms){
        for(int i = 0; i < atoms.length; i++){
            if(atoms[i].getId() == successorID){
                return i;
            }
        }
        return -1;
    }

    public void initSuccessors(UserStory parent) {
        val successorList = new Vector<Atom>();
        Arrays.stream(successors).forEach(successor -> {
            int successorIndex = getSuccessorIndex(successor, parent.getAtoms());
            if(successorIndex == -1){
                log.error("Could not find successor with id "+successor+" for atom \""+name+"\"");
                return;
            }
            successorList.add(parent.getAtoms()[successorIndex]);
            parent.getAtoms()[successorIndex].incrementPredecessorCount();
        });
        //boilerplate
        this.successorLinks = successorList.toArray(new Atom[0]);
        this.parent = parent;
    }

    private void runSuccessors() throws InterruptedException, ExecutionException {
        val threads = Arrays.stream(successorLinks).map(successorLink -> new Runnable(){
            @Override
            public void run() {
                try {
                    val clonedMap = new HashMap<String, String>(Atom.this.getKnownParams());
                    try {
                        successorLink.run(clonedMap);
                    } catch (ExecutionException e) {
                        log.error(e);
                    }
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }).collect(Collectors.toUnmodifiableList());
        val futures = threads.stream().map(runnable -> ThreadRecycler.getInstance().getExecutorService().submit(runnable)).collect(Collectors.toList());
        for(val thread : futures){
            if(!thread.isCancelled()) {
                thread.get();
            }
        }
    }

    /**
     * This is used to make sure that not 2 copies are created of an atom with 2 predecessors. See Test.
     * @return
     */
    protected abstract Atom performClone();
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
        atom.parent = this.parent;
        return atom;
    }
}
