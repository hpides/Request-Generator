package de.hpi.tdgt;

import de.hpi.tdgt.deserialisation.Deserializer;
import org.apache.commons.io.IOUtils;
import de.hpi.tdgt.test.Test;

import java.io.*;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0){
            try {
                System.err.println("Usage: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" <Path to request JSON>");
                System.exit(1);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(new FileInputStream(args[0]), writer);
            String json = writer.toString();
            Test deserializedTest = Deserializer.deserialize(json);
            System.out.println("Successfully deserialized input json including "+deserializedTest.getStories().length+" stories.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
