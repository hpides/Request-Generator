package de.hpi.tdgt.test.story.atom.assertion;

import de.hpi.tdgt.requesthandling.RestResult;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Log4j2
public class ResponseCode extends Assertion{
    private int responseCode;

    @Override
    public void check(RestResult restResult) {
        if(responseCode!=restResult.getReturnCode()){
            log.error("Failed response code assertion\""+getName()+"\": expected \""+responseCode+"\" but is actually \""+restResult.getReturnCode()+"\"!");
            AssertionStorage.getInstance().addFailure(this.getName());
            AssertionStorage.getInstance().addActual(this.getName(),""+restResult.getReturnCode());
        }
    }
}
