package de.hpi.tdgt.test.story.activity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Delay extends Activity{
    private int delayMs;

    @Override
    public void perform() {
        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
