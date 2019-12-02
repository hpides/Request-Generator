package de.hpi.tdgt.test.story.atom;

import lombok.*;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Log4j2
public class Delay extends Atom {
    private int delayMs;

    @Override
    public void perform() {
        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    @Override
    public Atom performClone() {
        val ret = new Delay();
        ret.setDelayMs(this.getDelayMs());
        return ret;
    }
}
