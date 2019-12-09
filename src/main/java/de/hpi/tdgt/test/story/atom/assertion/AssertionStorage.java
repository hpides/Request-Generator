package de.hpi.tdgt.test.story.atom.assertion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.Pair;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.Getter;
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
    private AssertionStorage() {
        String publisherId = UUID.randomUUID().toString();
        //we want to receive every packet EXACTLY Once
//to clean files
        mqttRunnable = () -> {
            //recommended way to make the thread stop
            while (running.get()) {
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
                    } catch (MqttException e) {
                        log.error("Could not connect to mqtt broker in AssertionStorage: ", e);
                        return;
                    }
                }
                //client is created and connected
                byte[] message = new byte[0];
                try {
                    synchronized (actualsLastSecond) {
                        message = AssertionStorage.this.mapper.writeValueAsString(actualsLastSecond).getBytes(StandardCharsets.UTF_8);
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
                    log.info(String.format("Transferred %d bytes via mqtt!", message.length));
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
        reporter = new Thread(mqttRunnable);
        reporter.start();
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

    public void addFailure(String assertionName, String actual) {
        //test was started after reset was called, so restart the thread
        if (reporter == null) {
            reporter = new Thread(mqttRunnable);
            log.info("Resumed reporter.");
            running.set(true);
            reporter.start();
        }
        Pair<Integer,Set<String>> pair;
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

    public Set<String> getActual(String assertionName) {
        return this.actuals.getOrDefault(assertionName, new Pair<>(0, new ConcurrentSkipListSet<>())).getValue();
    }

    public static final String MQTT_TOPIC = "de.hpi.tdgt.assertions";

}
