package de.hpi.tdgt.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;

import de.hpi.tdgt.test.story.UserStory;
@Getter
@Setter
@NoArgsConstructor
public class Test {
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;

    public void start() {
        Arrays.stream(stories).forEach(story -> story.getActivities()[0].run(new HashMap<String,String>()));
    }
}
