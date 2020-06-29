/*
 * WALT - A realistic load generator for web applications.
 *
 * Copyright 2020 Eric Ackermann <eric.ackermann@student.hpi.de>, Hendrik Bomhardt
 * <hendrik.bomhardt@student.hpi.de>, Benito Buchheim
 * <benito.buchheim@student.hpi.de>, Juergen Schlossbauer
 * <juergen.schlossbauer@student.hpi.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hpi.tdgt.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Properties;
@Log4j2
public class PropertiesReader {

    public static String BROKER_URL = null;
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

    public static Integer getThreadsPerCPU(){
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

    public static boolean AsyncIO(){
        return !(System.getenv("SYNC_IO")!= null && System.getenv("SYNC_IO").equals("true"));
    }
}
