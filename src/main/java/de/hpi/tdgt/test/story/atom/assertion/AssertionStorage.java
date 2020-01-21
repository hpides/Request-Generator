package de.hpi.tdgt.test.story.atom.assertion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.test.ThreadRecycler;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.Pair;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.Getter;
import lombok.Setter;
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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class AssertionStorage {
    private MqttClient client = null;
    private Thread reporter;
    private Runnable mqttRunnable;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private long testid = 0;
    private AssertionStorage() {
        String publisherId = UUID.randomUUID().toString();
        //we want to receive every packet EXACTLY Once
//to clean files
        mqttRunnable = () -> {
            //recommended way to make the thread stop
            while (running.get()) {
                //first second starts after start / first entry
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                //will be closed in reset, so might have to be re-created here
                if (client == null || ! client.isConnected()) {
                    try {
                        //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                        client = new MqttClient(PropertiesReader.getMqttHost(), publisherId, new MemoryPersistence());
                    } catch (MqttException e) {
                        log.error("Error creating mqttclient in AssertionStorage: ", e);
                        return;
                    }
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setAutomaticReconnect(true);
                    options.setCleanSession(true);
                    options.setConnectionTimeout(10);
                    try {
                        client.connect(options);
                        //clear retained messages from last test
                        client.publish(MQTT_TOPIC, new byte[0],0,true);
                    } catch (MqttException e) {
                        log.error("Could not connect to mqtt broker in AssertionStorage: ", e);
                        break;
                    }
                }
                //client is created and connected
                byte[] message = new byte[0];
                try {
                    synchronized (actualsLastSecond) {
                        message = (AssertionStorage.this.mapper.writeValueAsString(new MqttAssertionMessage(testid, this.actualsLastSecond))).getBytes(StandardCharsets.UTF_8);
                        AssertionStorage.this.actualsLastSecond.clear();
                    }
                } catch (JsonProcessingException e) {
                    log.error(e);
                }
                MqttMessage mqttMessage = new MqttMessage(message);
                //we want to receive every packet EXACTLY Once
                mqttMessage.setQos(2);
                mqttMessage.setRetained(true);
                try {
                    client.publish(AssertionStorage.MQTT_TOPIC, mqttMessage);
                    log.info(String.format("Transferred %d bytes via mqtt to "+MQTT_TOPIC, message.length));
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
                client = null;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        };
    }

    @Getter
    public static final AssertionStorage instance = new AssertionStorage();
    private final Map<String, Pair<Integer, Set<String>>> actuals = new ConcurrentHashMap<>();
    //should only be used for tests
    @Getter
    private final Map<String, Pair<Integer, Set<String>>> actualsLastSecond = new ConcurrentHashMap<>();

    public int getFails(String assertionName) {
        return actuals.getOrDefault(assertionName, new Pair<>(0, new ConcurrentSkipListSet<>())).getKey();
    }

    @JsonIgnore
    private ObjectMapper mapper = new ObjectMapper();
    /**
     * If true, times are stored asynch. Else times are stored synchronously.
     */
    @Getter
    @Setter
    private boolean storeEntriesAsynch = true;
    public void addFailure(String assertionName, String actual, long testid) {
        //we can assume there is just ne test running at any given time, so this is sufficient
        this.testid = testid;
        if(storeEntriesAsynch) {
            //needs quite some synchronization time and might run some time, so run it async if possible
            /*ThreadRecycler.getInstance().getExecutorService().submit(() -> {
                doAddFailure(assertionName, actual);

            });*/
        }
        else {
            doAddFailure(assertionName, actual);
        }
    }

    private void doAddFailure(String assertionName, String actual) {
        //test was started after reset was called, so restart the thread
        if (reporter == null) {
            reporter = new Thread(mqttRunnable);
            log.info("Resumed reporter.");
            running.set(true);
            reporter.start();
        }
        Pair<Integer, Set<String>> pair;
        synchronized (this) {
            pair = actuals.getOrDefault(assertionName, new Pair<>(0, new ConcurrentSkipListSet<>()));
            int current = pair.getKey();
            pair.setKey(current + 1);
            actuals.put(assertionName, pair);
        }
        synchronized (actualsLastSecond) {
            actualsLastSecond.put(assertionName, pair);
        }
        addActual(assertionName, actual);
    }

    public void reset() {
        running.set(false);
        //reset might be called twice
        if (reporter != null) {
            reporter.interrupt();
        }
        reporter = null;
        actuals.clear();
        actualsLastSecond.clear();
    }

    /**
     * Prints nice human readable summary to the console
     */
    public void printSummary() {
        for (val entry : actuals.entrySet()) {
            log.info("Assertion " + entry.getKey() + " failed " + entry.getValue().getKey() + " times.");
            if (entry.getValue().getKey() > 0) {
                StringBuilder actuals = new StringBuilder("Actual values: [");
                boolean first = true;
                for (val actual : this.actuals.getOrDefault(entry.getKey(), new Pair<>(0, new ConcurrentSkipListSet<>())).getValue()) {
                    if (!first) {
                        actuals.append(", ");
                    }
                    first = false;
                    actuals.append(actual);
                }
                actuals.append("]");
                log.info(actuals);

            }
        }
    }

    /**
     * Store unexpected value.
     * @param assertionName Name of the assertion that failed
     * @param value actual value
     */
    private void addActual(String assertionName, String value) {

            this.actuals.putIfAbsent(assertionName, new Pair<>());
            this.actualsLastSecond.putIfAbsent(assertionName, new Pair<>());
            var actuals = this.actuals.get(assertionName).getValue();
            if (actuals != null) {
                actuals.add(value);
            }
            synchronized (this.actualsLastSecond) {
                actuals = this.actualsLastSecond.getOrDefault(assertionName, new Pair<>()).getValue();
                if (actuals != null) {
                    actuals.add(value);
                }
            }
    }

    /**
     * Return all unexpected values during the test.
     * @param assertionName Name of he assertion.
     * @return Every value exactly once.
     */
    public Set<String> getActual(String assertionName) {
        return this.actuals.getOrDefault(assertionName, new Pair<>(0, new ConcurrentSkipListSet<>())).getValue();
    }

    public static final String MQTT_TOPIC = "de.hpi.tdgt.assertions";

}
