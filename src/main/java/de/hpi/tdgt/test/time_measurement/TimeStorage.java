package de.hpi.tdgt.test.time_measurement;

import de.hpi.tdgt.util.PropertiesReader;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Log4j2
public class TimeStorage {
    private MqttClient client = null;
    protected TimeStorage(){
        String publisherId = UUID.randomUUID().toString();
        try {
            client = new MqttClient(PropertiesReader.getMqttHost(),publisherId);
        } catch (MqttException e) {
            log.error("Error creating mqttclient in TimeStorage: ",e);
            return;
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        try {
            client.connect(options);
        } catch (MqttException e) {
            log.error("Could not connect to mqtt broker in TimeStorage: ",e);
        }
    }

    private final Map<String, Map<String, List<Long>>> registeredTimes = new ConcurrentHashMap<>();

    private static final TimeStorage storage = new TimeStorage();

    public static TimeStorage getInstance(){
        return storage;
    }

    public void registerTime(String verb, String addr, long latency){
        registeredTimes.computeIfAbsent(addr, k -> new ConcurrentHashMap<>());
        registeredTimes.get(addr).computeIfAbsent(verb, k-> new Vector<>());
        registeredTimes.get(addr).get(verb).add(latency);
        if(client != null && client.isConnected()){
            val message = String.format("{\"Time\":%d,\"addr\":\"%s\",\"verb\":\"%s\"}", latency, addr, verb).getBytes();
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

    public Long[] getTimes(String verb, String addr){
        //stub
        if(registeredTimes.get(addr) == null){
            return new Long[0];
        }
        if(registeredTimes.get(addr).get(verb) == null){
            return new Long[0];
        }
        return registeredTimes.get(addr).get(verb).toArray(new Long[0]);
    }


    public Long getMax(String verb, String addr){
        Long[] values = getTimes(verb,addr);
        long max = 0;
        for(long value:values){
            if(max < value){
                max = value;
            }
        }
        return max;
    }

    public long getMin(String verb, String addr){
        Long[] values = getTimes(verb,addr);
        long min = Long.MAX_VALUE;
        for(long value:values){
            if(min > value){
                min = value;
            }
        }
        return min;
    }

    public double getAvg(String verb, String addr){
        Long[] values = getTimes(verb,addr);
        double sum = 0;
        for(long value:values){
            sum += value;
        }
        return sum / values.length;
    }
    private static final double MS_IN_NS = 1000000d;
    public void printSummary(){
        for(val entry : registeredTimes.entrySet()){
            for(val verbMap : entry.getValue().entrySet()) {
                log.info("Endpoint " +verbMap.getKey()+ " " +entry.getKey()+ " min: " + getMin(verbMap.getKey(), entry.getKey()) / MS_IN_NS+" ms, max: "+getMax(verbMap.getKey(), entry.getKey())/MS_IN_NS+" ms, avg: "+getAvg(verbMap.getKey(), entry.getKey())/MS_IN_NS+" ms.");
            }
        }
    }

    public static final String MQTT_TOPIC = "de.hpi.tdgt.time";


}
