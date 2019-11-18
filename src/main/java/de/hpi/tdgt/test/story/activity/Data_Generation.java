package de.hpi.tdgt.test.story.activity;

import java.util.HashMap;
import java.util.Map;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
public class Data_Generation extends Activity {
    private String[] data;
    private String table;

    @Override
    public void perform() {
        val generatedData = readBuffer();
        this.getKnownParams().putAll(generatedData);
    }

    public Map<String,String> readBuffer() {
        val buffer = new HashMap<String,String>();
        buffer.put("key", "user");
        buffer.put("value", "pw");
        return buffer;
    }
}
