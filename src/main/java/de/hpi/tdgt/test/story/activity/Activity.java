package de.hpi.tdgt.test.story.activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import de.hpi.tdgt.test.story.UserStory;

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
})
@EqualsAndHashCode
public abstract class Activity {
    private String name;
    private int id;

    //should not have Getter, but needs Setter for Jackson
    @Getter(AccessLevel.NONE)
    private int[] successors;

    @JsonIgnore
    private int predecessorCount = 0;
    @JsonIgnore
    private int predecessorsReady = 0;

    @JsonIgnore
    @Getter
    private final Map<String,String> knownParams = new HashMap<String,String>();

    //do not try to read this from json, no accessors --> only used internally
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Activity[] successorLinks = new Activity[0];
    private int repeat;

    public abstract void perform();
    //use this method to get successors of this activity
    public Activity[] getSuccessors() {
        return successorLinks;
    }

    public void run(Map<String,String> dataMap) {
        predecessorsReady++;
        this.getKnownParams().putAll(dataMap);
        if (predecessorsReady >= predecessorCount) {
            perform();
            runSuccessors();
        }
    }

    public void incrementPredecessorCount() {
        predecessorCount++;
    }

    public void initSuccessors(UserStory parent) {
        val successorList = new Vector<Activity>();
        Arrays.stream(successors).forEach(successor -> {
            successorList.add(parent.getActivities()[successor]);
            parent.getActivities()[successor].incrementPredecessorCount();
        });
        //boilerplate
        this.successorLinks = successorList.toArray(new Activity[0]);
    }

    private void runSuccessors() {
        Arrays.stream(successorLinks).forEach(successorLink -> successorLink.run(this.getKnownParams()));
    }

    protected abstract Activity performClone();

    @Override
    public Activity clone(){
        val activity = performClone();
        //do NOT clone predecessorsReady or knownParams
        activity.setId(this.getId());
        activity.setName(this.getName());
        activity.setPredecessorCount(this.getPredecessorCount());
        activity.setRepeat(this.getRepeat());
        activity.successorLinks = Arrays.stream(this.getSuccessors()).map(Activity::clone).toArray(Activity[]::new);
        return activity;
    }
}
