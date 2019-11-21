package de.hpi.tdgt.test.story.activity;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
public class Data_Generation extends Activity {
    private String[] data;
    private String table;
    private InputStream stream;
    private static final Map<String, Scanner> association = new HashMap<>();
    private Scanner sc;
    @Override
    public void perform() {
        val generatedData = readBuffer();
        this.getKnownParams().putAll(generatedData);
    }

    public Map<String, String> readBuffer() {
        if (stream != null) {
            initScanner();
            //sc has a value now
            val buffer = new HashMap<String, String>();
            //scanner is not thread save, but will only be assigned once. So we can synchronise on it.
            //this sync prevents mixups when calling nextLine, e.g. two threads call it at the same time when only one Line remaining
            String line;
            synchronized (sc) {
                if (sc.hasNextLine()) {
                    line = sc.nextLine();
                } else {
                    System.err.println("No data remains!");
                    return buffer;
                }
                // Scanner suppresses exceptions
                if (sc.ioException() != null) {
                    System.err.println("Exception: ");
                    sc.ioException().printStackTrace();
                }
            }
            //can be done without synchronisation, saves time spent in sequential mode
            String[] values = line.split(";");
            if (values.length < this.getData().length) {
                System.err.println("Generated data does not match required data!");
            } else {
                for (int i = 0; i < data.length; i++) {
                    buffer.put(data[i], values[i]);
                }
            }

            return buffer;
        } else {
            val buffer = new HashMap<String, String>();
            buffer.put("key", "user");
            buffer.put("value", "pw");
            return buffer;
        }
    }

    private void initScanner() {
        //only one Thread is allowed to add a scanner at the same time; only need to synchronise scanner creation and retrieval
        if (sc == null) {
            synchronized (association) {
                if (association.containsKey(table)) {
                    sc = association.get(table);
                } else {
                    sc = new Scanner(this.getStream(), "UTF-8");
                    association.put(table, sc);
                }
            }
        }
    }
}
