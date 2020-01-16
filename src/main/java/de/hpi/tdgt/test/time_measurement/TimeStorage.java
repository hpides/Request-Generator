package de.hpi.tdgt.test.time_measurement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.ThreadRecycler;
import de.hpi.tdgt.util.Pair;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class TimeStorage {
    //can't be re-connected, create a new instance everytime the thread is run
    private MqttClient client;
    private Thread reporter = null;
    private Runnable mqttReporter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private long testID = 0;
    /**
     * Flag for tests. If true, only messages that contain times are sent.
     */
    @Setter
    private boolean sendOnlyNonEmpty = false;
    protected TimeStorage() {
        //to clean files
        mqttReporter = () -> {
            while (running.get()) {
                //first second starts after start / first entry
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //Clean up
                    break;
                }
                String publisherId = UUID.randomUUID().toString();
                try {
                    //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                    client = new MqttClient(PropertiesReader.getMqttHost(), publisherId, new MemoryPersistence());
                } catch (MqttException e) {
                    log.error("Error creating mqttclient in TimeStorage: ", e);
                }
                //client is null if reset was called
                if (client != null && !client.isConnected()) {

                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setAutomaticReconnect(true);
                    options.setCleanSession(true);
                    options.setConnectionTimeout(10);
                    try {
                        client.connect(options);
                        //clear retained messages from last test
                        client.publish(MQTT_TOPIC, new byte[0],0,true);
                    } catch (MqttException e) {
                        log.error("Could not connect to mqtt broker in TimeStorage: ", e);
                        //clean up
                        break;
                    }
                }
                //client is created and connected

                //prevent error
                byte[] message = new byte[0];
                try {
                    //needs to be synchronized so we do not miss entries
                    synchronized (registeredTimesLastSecond) {
                        val entry = toMQTTSummaryMap(registeredTimesLastSecond);
                        // tests only want actual times
                        if(!sendOnlyNonEmpty || (entry.getTimes() != null && !entry.getTimes().isEmpty())){
                            message = mapper.writeValueAsString(entry).getBytes(StandardCharsets.UTF_8);
                        }
                        registeredTimesLastSecond.clear();
                    }
                } catch (JsonProcessingException e) {
                    log.error(e);
                }
                MqttMessage mqttMessage = new MqttMessage(message);
                //we want to receive every packet EXACTLY once
                mqttMessage.setQos(2);
                mqttMessage.setRetained(true);
                try {
                    if(client != null) {
                        client.publish(MQTT_TOPIC, mqttMessage);
                    }
                    log.trace(String.format("Transferred %d bytes via mqtt!", message.length));
                } catch (MqttException e) {
                    log.error("Error sending mqtt message in Time_Storage: ", e);
                }
            }
            //to clean files
            try {
                if(client != null) {
                    //clear retained messages for next test
                    client.publish(MQTT_TOPIC, new byte[0],0,true);
                    client.disconnect();
                    client.close();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        };
        reporter = new Thread(mqttReporter);
        reporter.setPriority(Thread.MAX_PRIORITY);
        reporter.start();
    }

    /**
     * Outermost String is the request.
     * Second string from outside is the method.
     * Innermost String is story the request belonged to.
     */
    private final Map<String, Map<String, Map<String, List<Long>>>> registeredTimes = new ConcurrentHashMap<>();
    /**
     * Outermost String is the request.
     * Second string from outside is the method.
     * Innermost String is story the request belonged to.
     */
    private final Map<String, Map<String, Map<String, List<Long>>>> registeredTimesLastSecond = new ConcurrentHashMap<>();

    private static final TimeStorage storage = new TimeStorage();

    public static TimeStorage getInstance() {
        return storage;
    }
    public static final String THROUGHPUT_STRING="throughput";
    public static final String MIN_LATENCY_STRING="minLatency";
    public static final String MAX_LATENCY_STRING="maxLatency";
    public static final String AVG_LATENCY_STRING="avgLatency";
    public static final String STORY_STRING="story";
    private MqttTimeMessage toMQTTSummaryMap(Map<String, Map<String, Map<String, List<Long>>>> currentValues) {
        log.trace("Is empty: " + currentValues.isEmpty());
        Map<String, Map<String, Map<String, Map<String, String>>>> ret = new HashMap<>();
        //re-create the structure, but using average of the innermost values
        for (val entry : currentValues.entrySet()) {
            ret.put(entry.getKey(), new HashMap<>());
            for (val innerEntry : entry.getValue().entrySet()) {
                ret.get(entry.getKey()).put(innerEntry.getKey(), new HashMap<>());
                for(val innermostEntry : innerEntry.getValue().entrySet()) {
                    double avg = innermostEntry.getValue().stream().mapToLong(Long::longValue).average().orElse(0);
                    long min = innermostEntry.getValue().stream().mapToLong(Long::longValue).min().orElse(0);
                    long max = innermostEntry.getValue().stream().mapToLong(Long::longValue).max().orElse(0);
                    //number of times this request was sent this second
                    long throughput = innermostEntry.getValue().size();
                    HashMap<String, String> times = new HashMap<>();
                    times.put(THROUGHPUT_STRING, "" + throughput);
                    times.put(MIN_LATENCY_STRING, "" + min);
                    times.put(MAX_LATENCY_STRING, "" + max);
                    NumberFormat nf_out = NumberFormat.getNumberInstance(Locale.UK);
                    nf_out.setGroupingUsed(false);
                    times.put(AVG_LATENCY_STRING, nf_out.format(avg));
                    ret.get(entry.getKey()).get(innerEntry.getKey()).put(innermostEntry.getKey(), times);
                }
            }
        }
        val entry = new MqttTimeMessage();
        entry.setTestId(testID);
        entry.setCreationTime(System.currentTimeMillis());
        entry.setTimes(ret);
        return entry;
    }

    private ObjectMapper mapper = new ObjectMapper();
    public void registerTime(String verb, String addr, long latency, String story) {
        //test was started after reset was called, so restart the thread
        if (reporter == null) {
            reporter = new Thread(mqttReporter);
            log.info("Resumed reporter.");
            running.set(true);
            reporter.setPriority(Thread.MAX_PRIORITY);
            reporter.start();
        }
        //triggers exception
        if(story != null) {
            registeredTimes.computeIfAbsent(addr, k -> new ConcurrentHashMap<>());
            registeredTimes.get(addr).computeIfAbsent(verb, k -> new ConcurrentHashMap<>());
            registeredTimes.get(addr).get(verb).computeIfAbsent(story, k -> new Vector<>()).add(latency);
            synchronized (registeredTimesLastSecond) {
                registeredTimesLastSecond.computeIfAbsent(addr, k -> new ConcurrentHashMap<>());
                registeredTimesLastSecond.get(addr).computeIfAbsent(verb, k -> new ConcurrentHashMap<>());
                registeredTimesLastSecond.get(addr).get(verb).computeIfAbsent(story, k -> new Vector<>()).add(latency);
            }
        }
    }

    /**
     * Times for a certain endpoint.
     * @param verb Like POST, GET, ...
     * @param addr Endpoint
     * @return Array with all times
     */
    public Long[] getTimes(String verb, String addr) {
        //stub
        if (registeredTimes.get(addr) == null) {
            return new Long[0];
        }
        if (registeredTimes.get(addr).get(verb) == null) {
            return new Long[0];
        }
        val allTimes = new Vector<Long>();
        for(val entry : registeredTimes.get(addr).get(verb).entrySet()){
            allTimes.addAll(entry.getValue());
        }
        return allTimes.toArray(new Long[0]);
    }

    // min, max, avg over the complete run or 0 if can not be computed
    public Long getMax(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        return Arrays.stream(values).mapToLong(Long::longValue).max().orElse(0);
    }

    public long getMin(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        return Arrays.stream(values).mapToLong(Long::longValue).min().orElse(0);
    }

    public double getAvg(String verb, String addr) {
        Long[] values = getTimes(verb, addr);
        return Arrays.stream(values).mapToLong(Long::longValue).average().orElse(0);
    }

    private static final double MS_IN_NS = 1000000d;

    /**
     * Print nice summry to the console
     */
    public void printSummary() {
        for (val entry : registeredTimes.entrySet()) {
            for (val verbMap : entry.getValue().entrySet()) {
                log.info("Endpoint " + verbMap.getKey() + " " + entry.getKey() + " min: " + getMin(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms, max: " + getMax(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms, avg: " + getAvg(verbMap.getKey(), entry.getKey()) / MS_IN_NS + " ms.");
            }
        }
    }

    public static final String MQTT_TOPIC = "de.hpi.tdgt.times";

    public void reset() {
        //reset might be called twice, so make sure we do not encounter Nullpointer
        if (reporter != null) {
            running.set(false);
            reporter.interrupt();
        }
        reporter = null;
        registeredTimesLastSecond.clear();
        registeredTimes.clear();
    }


}
