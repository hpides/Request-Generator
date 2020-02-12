package de.hpi.tdgt.test.story.atom.assertion;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.hpi.tdgt.requesthandling.RestResult;
import lombok.*;
import lombok.extern.log4j.Log4j2;

//tell Jackson to use subclasses by type attribute
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
//tell Jackson which subtypes are allowed
@JsonSubTypes({
        @JsonSubTypes.Type(value = ContentNotEmpty.class, name = "CONTENT_NOT_EMPTY"),
        @JsonSubTypes.Type(value = ContentType.class, name = "CONTENT_TYPE"),
        @JsonSubTypes.Type(value = ResponseCode.class, name = "RESPONSE_CODE"),
})
@EqualsAndHashCode
@Log4j2
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Assertion {
    private String name;

    public abstract void check(RestResult restResult, long testid);
}
