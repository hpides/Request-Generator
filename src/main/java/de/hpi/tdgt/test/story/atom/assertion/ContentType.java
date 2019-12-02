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
public class ContentType extends Assertion {
    private String contentType;

    @Override
    public void check(RestResult restResult) {
        if(!contentType.equals(restResult.getContentType())){
            log.error("Failed content type assertion\""+getName()+"\": expected \""+contentType+"\" but is actually \""+restResult.getContentType()+"\"!");
            AssertionStorage.getInstance().addFailure(this.getName());
            AssertionStorage.getInstance().addActual(this.getName(),restResult.getContentType());
        }
    }
}
