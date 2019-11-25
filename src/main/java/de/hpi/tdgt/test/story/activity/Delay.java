package de.hpi.tdgt.test.story.activity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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

    @Override
    public Activity performClone() {
        val ret = new Delay();
        ret.setDelayMs(this.getDelayMs());
        return ret;
    }
}
