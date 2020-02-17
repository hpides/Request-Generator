package de.hpi.tdgt.test.story.atom.assertion;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.hpi.tdgt.requesthandling.RestResult;
import org.apache.logging.log4j.Logger;

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
public abstract class Assertion {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Assertion.class);
    private String name;

    public Assertion(String name) {
        this.name = name;
    }

    public Assertion() {
    }

    public abstract void check(RestResult restResult, long testid);

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Assertion)) return false;
        final Assertion other = (Assertion) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Assertion;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }
}
