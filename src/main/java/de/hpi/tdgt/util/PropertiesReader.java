package de.hpi.tdgt.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Properties;
@Log4j2
public class PropertiesReader {
    public static String getMqttHost(){
        Properties prop = new Properties();
        try {
            //load a properties file from class path, inside static method
            prop.load(PropertiesReader.class.getClassLoader().getResourceAsStream("application.properties"));

            return prop.getProperty("mqtt.host");

        }
        catch (IOException ex) {
            log.error("Could not get mqtt host: ",ex);
        }
        return null;
    }

    public static int getThreadsPerCPU(){
        Properties prop = new Properties();
        try {
            //load a properties file from class path, inside static method
            prop.load(PropertiesReader.class.getClassLoader().getResourceAsStream("application.properties"));

            return Integer.valueOf(prop.getProperty("threads.per.cpu"));

        }
        catch (IOException ex) {
            log.error("Could not get threads per cpu: ",ex);
        }
        return 1;
    }
}
