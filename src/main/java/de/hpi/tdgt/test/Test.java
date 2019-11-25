package de.hpi.tdgt.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Vector;

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
        val threads = new Vector<Thread>();
        for(int i=0; i < stories.length; i++){
            //repeat stories as often as wished
            for(int j = 0; j < scaleFactor * stories[i].getScalePercentage(); j++) {
                val thread = new Thread(stories[i]);
                thread.start();
                threads.add(thread);
            }
        }
        for(val thread : threads){
            thread.join();
        }
    }
}
