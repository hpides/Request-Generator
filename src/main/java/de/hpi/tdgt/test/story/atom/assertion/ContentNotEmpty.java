package de.hpi.tdgt.test.story.atom.assertion;

import de.hpi.tdgt.requesthandling.RestResult;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Log4j2
public class ContentNotEmpty extends Assertion {
    @Override
    public void check(RestResult restResult) {
        if(restResult.getResponse().length == 0){
            log.error("Failed content not empty assertion\""+getName()+"\": response was empty!");
            AssertionStorage.getInstance().addFailure(this.getName(), "");
        }
    }
}
