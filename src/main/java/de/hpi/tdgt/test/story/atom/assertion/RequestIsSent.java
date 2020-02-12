package de.hpi.tdgt.test.story.atom.assertion;

import de.hpi.tdgt.requesthandling.RestResult;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Log4j2
public class RequestIsSent extends Assertion {
    @Override
    public void check(RestResult restResult, long testid) {
        if(restResult.getErrorCondition() != null){
            log.error("Failed request is sent assertion\""+getName()+"\": "+restResult.getErrorCondition().getMessage());
            AssertionStorage.getInstance().addFailure(this.getName(), restResult.getErrorCondition().getClass().getName() + ":"+ restResult.getErrorCondition().getMessage(), testid);
        }
    }
}
