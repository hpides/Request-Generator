package test.story;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import test.story.activity.Activity;
@Getter
@Setter
@NoArgsConstructor
public class UserStory {
    private double scalePercentage;
    private String name;
    private Activity[] activities;
}
