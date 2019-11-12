package deserialisation;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.StringWriter;

public class Utils {
    public String getExampleJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("test_config_example.json"), writer);
        return writer.toString();
    }
    public static void assertInstanceOf(Object o, Class c){
        Assertions.assertTrue(c.isInstance(o), "First activity of first story should be a data generation activity and not a "+o.getClass().getName());
    }
}
