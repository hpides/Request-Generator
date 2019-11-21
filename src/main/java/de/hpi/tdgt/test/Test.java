package de.hpi.tdgt.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;

import de.hpi.tdgt.test.story.UserStory;
import lombok.val;

@Getter
@Setter
@NoArgsConstructor
public class Test {
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;

    public void start() throws InterruptedException {
        Thread[] threads = new Thread[stories.length];
        for(int i=0; i < stories.length; i++){
            threads[i] = new Thread(stories[i]);
            threads[i].start();
        }
        for(val thread : threads){
            thread.join();
        }
    }
}
