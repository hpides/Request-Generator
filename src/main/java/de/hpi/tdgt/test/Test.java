package de.hpi.tdgt.test;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import de.hpi.tdgt.test.story.UserStory;
@Getter
@Setter
@NoArgsConstructor
public class Test {
    private int repeat;
    private int scaleFactor;
    private UserStory[] stories;
}
