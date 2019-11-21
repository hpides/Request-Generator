package de.hpi.tdgt.test.story;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import de.hpi.tdgt.test.story.activity.Activity;

import java.util.Arrays;
import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
public class UserStory implements Runnable{
    private double scalePercentage;
    private String name;
    private Activity[] activities;
    public void setActivities(Activity[] activities){
        this.activities = activities;
        //set links
        Arrays.stream(activities).forEach(activity -> activity.initSuccessors(this));
    }

    @Override
    public void run() {
        activities[0].run(new HashMap<>());
    }
}
