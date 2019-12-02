package de.hpi.tdgt.test.story.atom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.log4j.Log4j2;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Log4j2
public class Data_Generation extends Atom {
    private String[] data;
    private String table;

    //should not be serialized or accessible from other classes
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private InputStream stream;

    //this is used to synchronise current line in all file(s)
    @JsonIgnore
    private static final Map<String, Scanner> association = new HashMap<>();

    /**
     * Removes all class state.
     * Instances will start reading files from the beginning again.
     */
    public static void reset() {
        //close all Scanners
        for (val scanner : association.values()) {
            scanner.close();
        }
        association.clear();
    }

    //should not be serialized or accessible from other classes
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Scanner sc;

    @Override
    public void perform() {
        val generatedData = readBuffer();
        this.getKnownParams().putAll(generatedData);
    }

    @Override
    public Atom performClone() {
        val ret = new Data_Generation();
        ret.setTable(this.table);
        ret.setData(this.data);
        return ret;
    }

    public Map<String, String> readBuffer() {
        initStream();
        initScanner();
        //sc has a value now
        val buffer = new HashMap<String, String>();
        //scanner is not thread save, but will only be assigned once. So we can synchronise on it.
        //this sync prevents mixups when calling nextLine, e.g. two threads call it at the same time when only one Line remaining
        String line;
        synchronized (sc) {
            if (sc.hasNextLine()) {
                line = sc.nextLine();
                log.info("Retrieved "+line+"from table"+" in Thread "+Thread.currentThread().getId()+ "for atom "+this.getName());
            } else {
                log.error("No data remains for atom "+this.getName());
                sc.close();
                return buffer;
            }
            // Scanner suppresses exceptions
            if (sc.ioException() != null) {
                log.error("Exception: ", sc.ioException());
            }
        }
        //can be done without synchronisation, saves time spent in sequential mode
        String[] values = line.split(";");
        if (values.length < this.getData().length -1) {
            log.error("Generated data does not match required data!");
        } else {
            for (int i = 0; i < data.length; i++) {
                //assume last csv column was empty
                if(i >= values.length){
                    buffer.put(data[data.length-1],"");
                }
                else {
                    buffer.put(data[i], values[i]);
                }
            }
        }

        return buffer;
    }

    public static String outputDirectory =".";
    private void initStream() {
        if (stream == null && table != null) {
            File table = new File(outputDirectory + "/"+ getTable() + ".csv");
            try {
                stream = new FileInputStream(table);
            } catch (FileNotFoundException e) {
               log.error(e);
            }
        }
    }

    private void initScanner() {
        //only one Thread is allowed to add a scanner at the same time; only need to synchronise scanner creation and retrieval
        if (sc == null) {
            synchronized (association) {
                if (association.containsKey(table)) {
                    sc = association.get(table);
                } else {
                    sc = new Scanner(stream, "UTF-8");
                    association.put(table, sc);
                }
            }
        }
    }
}
