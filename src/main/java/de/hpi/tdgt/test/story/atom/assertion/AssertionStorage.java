package de.hpi.tdgt.test.story.atom.assertion;

import de.hpi.tdgt.util.PropertiesReader;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Log4j2
public class AssertionStorage {
    private MqttClient client = null;
    private AssertionStorage(){
        String publisherId = UUID.randomUUID().toString();
        try {
            client = new MqttClient(PropertiesReader.getMqttHost(),publisherId);
        } catch (MqttException e) {
            log.error("Error creating mqttclient in AssertionStorage: ",e);
            return;
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        try {
            client.connect(options);
        } catch (MqttException e) {
            log.error("Could not connect to mqtt broker in AssertionStorage: ",e);
        }
    }
    @Getter
    public static final AssertionStorage instance = new AssertionStorage();

    private final Map<String, Integer> fails = new HashMap<>();
    private final Map<String, Set<String>> actuals = new ConcurrentHashMap<>();

    public int getFails(String assertionName){
        return fails.getOrDefault(assertionName, 0);
    }

    public void addFailure(String assertionName, String actual){
        synchronized (this) {
            int current = fails.getOrDefault(assertionName, 0);
            current++;
            fails.put(assertionName, current);
        }
        addActual(assertionName, actual);
        if(client != null && client.isConnected()){
            val message = String.format("{\"name\":\"%s\",\"actual\":\"%s\"}", assertionName, actual).getBytes();
            MqttMessage mqttMessage = new MqttMessage(message);
            //we want to receive every packet EXACTLY Once
            mqttMessage.setQos(2);
            mqttMessage.setRetained(true);
            try {
                client.publish(MQTT_TOPIC, mqttMessage);
                log.info(String.format("Transferred %d bytes via mqtt!",message.length));
            } catch (MqttException e) {
                log.error("Error sending mqtt message in Time_Storage: ", e);
            }
        }
    }

    public void reset(){
        fails.clear();
        actuals.clear();
    }

    public void printSummary(){
        for(val entry :fails.entrySet()){
            log.info("Assertion "+entry.getKey()+" failed "+entry.getValue()+" times.");
            if(entry.getValue() > 0){
                StringBuilder actuals = new StringBuilder("Actual values: [");
                boolean first = true;
                for(val actual : this.actuals.getOrDefault(entry.getKey(), new ConcurrentSkipListSet<>())){
                    if(!first){
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
    private void addActual(String assertionName, String value){
        this.actuals.putIfAbsent(assertionName, new ConcurrentSkipListSet<>());
        val actuals = this.actuals.get(assertionName);
        if (actuals != null) {
            actuals.add(value);
        }
    }
    public Set<String> getActual(String assertionName){
        return this.actuals.getOrDefault(assertionName, new ConcurrentSkipListSet<>());
    }
    public static final String MQTT_TOPIC = "de.hpi.tdgt.assertions";
}
