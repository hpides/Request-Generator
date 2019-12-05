package de.hpi.tdgt.mqtt;

import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.story.atom.assertion.ContentType;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
@Log4j2
public class MQTTTest extends RequestHandlingFramework {
    @Test
    public void TimeStorageStreamsTimesUsingMQTT() throws MqttException, InterruptedException {
        final StringBuilder message = prepareClient(TimeStorage.MQTT_TOPIC);
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10);
        Thread.sleep(3000);
        MatcherAssert.assertThat(message.toString(), Matchers.stringContainsInOrder("{\"Time\":10,\"addr\":\"http://localhost:9000/\",\"verb\":\"POST\"}"));
    }

    private StringBuilder prepareClient(final String topic) throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        IMqttClient publisher = new MqttClient(PropertiesReader.getMqttHost(),publisherId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);
        final StringBuilder message = new StringBuilder();
        publisher.subscribe(topic, (s, mqttMessage) -> {
            if(s.equals(topic)) {
                log.info("Received "+new String(mqttMessage.getPayload()));
                message.append(new String(mqttMessage.getPayload()));
            }
        });
        return message;
    }

    @Test
    public void ResponseCodeAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        final StringBuilder message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        getWithAuth.run(params);
        Thread.sleep(3000);
        MatcherAssert.assertThat(message.toString(), Matchers.stringContainsInOrder("{\"name\":\"auth does not return 401\",\"actual\":\"401\"}"));
    }
    @Test
    public void ContentTypeAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        final StringBuilder message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        val postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[1];
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        //simulate failure
        assertion.setContentType("application/xml");
        postWithBodyAndAssertion.run(params);
        Thread.sleep(3000);
        MatcherAssert.assertThat(message.toString(), Matchers.stringContainsInOrder("{\"name\":\"postWithBody returns JSON\",\"actual\":\"application/json\"}"));
    }
    @Test
    public void ResponseNotEmptyAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        final StringBuilder message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        Thread.sleep(3000);
        MatcherAssert.assertThat(message.toString(), Matchers.stringContainsInOrder("{\"name\":\"jsonObject returns something\",\"actual\":\"\"}"));
    }
}
