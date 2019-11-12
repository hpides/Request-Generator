package test.story.activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import test.story.UserStory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

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
        @JsonSubTypes.Type(value = Data_Generation.class, name = "DATA_GENERATION")
})
public abstract class Activity {
    private String name;
    private int id;

    //should not have Getter, but needs Setter for Jackson
    @Getter(AccessLevel.NONE)
    private int[] successors;

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

    public void initSuccessors(UserStory parent) {
        val successorList = new Vector<Activity>();
        Arrays.stream(successors).forEach(successor -> successorList.add(parent.getActivities()[successor]));
        //boilerplate
        this.successorLinks = successorList.toArray(new Activity[0]);
    }
}
