package test.story;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import test.story.activity.Activity;

import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class UserStory {
    private double scalePercentage;
    private String name;
    private Activity[] activities;
    public void setActivities(Activity[] activities){
        this.activities = activities;
        //set links
        Arrays.stream(activities).forEach(activity -> activity.initSuccessors(this));
    }
}
