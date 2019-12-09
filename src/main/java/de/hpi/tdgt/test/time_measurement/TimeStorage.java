package de.hpi.tdgt.test.time_measurement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class TimeStorage {
    private MqttClient client = null;
    private Thread reporter = null;
    private Runnable mqttReporter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    protected TimeStorage() {

        //we want to receive every packet EXACTLY Once
//to clean files
        mqttReporter = () -> {
            while (running.get() || ! client.isConnected()) {
                //client is null if reset was called
                if (client == null) {
                    String publisherId = UUID.randomUUID().toString();
                    try {
                        //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                        client = new MqttClient(PropertiesReader.getMqttHost(), publisherId, new MemoryPersistence());
                    } catch (MqttException e) {
                        log.error("Error creating mqttclient in TimeStorage: ", e);
                        return;
                    }
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setAutomaticReconnect(true);
                    options.setCleanSession(true);
                    options.setConnectionTimeout(10);
                    try {
                        client.connect(options);
                    } catch (MqttException e) {
                        log.error("Could not connect to mqtt broker in TimeStorage: ", e);
                        return;
                    }
                }
                //client is created and connected
                byte[] message = new byte[0];
                try {
                    synchronized (registeredTimesLastSecond) {
                        message = mapper.writeValueAsString(toMQTTSummaryMap(registeredTimesLastSecond)).getBytes(StandardCharsets.UTF_8);
                        registeredTimesLastSecond.clear();
                    }
                } catch (JsonProcessingException e) {
                    log.error(e);
                }
                MqttMessage mqttMessage = new MqttMessage(message);
                //we want to receive every packet EXACTLY Once
                mqttMessage.setQos(2);
                mqttMessage.setRetained(true);
                try {
                    client.publish(MQTT_TOPIC, mqttMessage);
                    log.trace(String.format("Transferred %d bytes via mqtt!", message.length));
                } catch (MqttException e) {
                    log.error("Error sending mqtt message in Time_Storage: ", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            //to clean files
            try {
                client.disconnect();
                client.close();
                client = null;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        };
        reporter = new Thread(mqttReporter);
        reporter.start();
    }

    private final Map<String, Map<String, List<Long>>> registeredTimes = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<String, List<Long>>> registeredTimesLastSecond = new ConcurrentHashMap<>();

    private static final TimeStorage storage = new TimeStorage();

    public static TimeStorage getInstance() {
        return storage;
    }

    private Map<String, Map<String, Double>> toMQTTSummaryMap(Map<String, ConcurrentHashMap<String, List<Long>>> currentValues) {
        log.trace("Is empty: " + currentValues.isEmpty());
        Map<String, Map<String, Double>> ret = new HashMap<>();
        //re-create the structure, but using average of the innermost values
        for (val entry : currentValues.entrySet()) {
            ret.put(entry.getKey(), new HashMap<>());
            for (val innerEntry : entry.getValue().entrySet()) {
                val sum = innerEntry.getValue().stream().mapToLong(Long::longValue).sum();
                double avg = sum / innerEntry.getValue().size();
                ret.get(entry.getKey()).put(innerEntry.getKey(), avg);
            }
        }
        return ret;
    }

    private ObjectMapper mapper = new ObjectMapper();

    public void registerTime(String verb, String addr, long latency) {
        //test was started after reset was called, so restart the thread
        if (reporter == null) {
            reporter = new Thread(mqttReporter);
            log.info("Resumed reporter.");
            running.set(true);
            reporter.start();
        }
        registeredTimes.computeIfAbsent(addr, k -> new ConcurrentHashMap<>());
        registeredTimes.get(addr).computeIfAbsent(verb, k -> new Vector<>());
        registeredTimes.get(addr).get(verb).add(latency);
        synchronized (registeredTimesLastSecond) {
            registeredTimesLastSecond.computeIfAbsent(addr, k -> new ConcurrentHashMap<>());
            registeredTimesLastSecond.get(addr).computeIfAbsent(verb, k -> new Vector<>());
            registeredTimesLastSecond.get(addr).get(verb).add(latency);
            log.info("Added val: " + registeredTimesLastSecond.isEmpty());
        }
    }

    public Long[] getTimes(String verb, String addr) {
        //stub
        if (registeredTimes.get(addr) == null) {
            return new Long[0];
        }
        if (registeredTimes.get(addr).get(verb) == null) {
            return new Long[0];
        }
        return registeredTimes.get(addr).get(verb).toArray(new Long[0]);
    }


    public Long getMax(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        long max = 0;
        for (long value : values) {
            if (max < value) {
                max = value;
            }
        }
        return max;
    }

    public long getMin(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        long min = Long.MAX_VALUE;
        for (long value : values) {
            if (min > value) {
                min = value;
            }
        }
        return min;
    }

    public double getAvg(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        double sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static final double MS_IN_NS = 1000000d;

    public void printSummary() {
        for (val entry : registeredTimes.entrySet()) {
            for (val verbMap : entry.getValue().entrySet()) {
                log.info("Endpoint " + verbMap.getKey() + " " + entry.getKey() + " min: " + getMin(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms, max: " + getMax(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms, avg: " + getAvg(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms.");
            }
        }
    }

    public static final String MQTT_TOPIC = "de.hpi.tdgt.times";

    public void reset() {
        //reset might be called twice
        if (reporter != null) {
            running.set(false);
            reporter.interrupt();
        }
        reporter = null;
        registeredTimesLastSecond.clear();
        registeredTimes.clear();
    }

}
