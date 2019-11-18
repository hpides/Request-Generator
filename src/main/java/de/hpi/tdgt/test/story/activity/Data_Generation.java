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
    public Map<String,String> perform(Map<String,String> dataMap) {
        val generatedData = readBuffer();
        generatedData.putAll(dataMap);
        return generatedData;
    }

    public Map<String,String> readBuffer() {
        val buffer = new HashMap<String,String>();
        buffer.put("key", "user");
        buffer.put("value", "pw");
        return buffer;
    }
}
