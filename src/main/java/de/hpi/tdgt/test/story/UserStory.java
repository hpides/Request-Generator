package de.hpi.tdgt.test.story;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import de.hpi.tdgt.test.story.activity.Activity;
import lombok.val;

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
    public UserStory clone(){
        val story = new UserStory();
        story.setName(this.getName());
        story.setScalePercentage(this.getScalePercentage());
        story.activities = new Activity[activities.length];
        //make a clone for all local changes, e.g. predecessorsReady
        for(int i = 0; i < activities.length; i++){
            story.getActivities()[i] = activities[i].clone();
        }
        //fix references
        Arrays.stream(story.getActivities()).forEach(activity -> activity.initSuccessors(story));
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
                System.out.println("Running story "+clone.getName()+" in thread "+Thread.currentThread().getId());
                clone.getActivities()[0].run(new HashMap<>());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        storyThread.start();
        try {
            storyThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
