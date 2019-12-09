package de.hpi.tdgt.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.atom.Atom;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.story.atom.assertion.ContentType;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.Pair;
import de.hpi.tdgt.util.PropertiesReader;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
@Log4j2
public class MQTTTest extends RequestHandlingFramework {
    private ObjectMapper mapper = new ObjectMapper();
    private IMqttClient publisher;
    @BeforeEach
    public void beforeEach(){
        //this test MUST handle asynch behaviour
        AssertionStorage.getInstance().setStoreEntriesAsynch(true);
        TimeStorage.getInstance().setStoreEntriesAsynch(true);
    }
    @Test
    public void TimeStorageStreamsTimesUsingMQTT() throws MqttException, InterruptedException, IOException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10);
        Thread.sleep(3000);
        TypeReference<HashMap<String, HashMap<String, HashMap<String, Double>>>> typeRef = new TypeReference<>() {};
        val response = new Vector<HashMap<String, HashMap<String, HashMap<String, Double>>>>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }

        MatcherAssert.assertThat(response, Matchers.hasItem(Matchers.hasKey("http://localhost:9000/")));

           }

    @Test
    public void TimeStorageStreamsAllTimesUsingMQTT() throws MqttException, InterruptedException, IOException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10);
        Thread.sleep(3000);
        TypeReference<HashMap<String, HashMap<String, HashMap<String, Double>>>> typeRef = new TypeReference<>() {};
        val response = new Vector<HashMap<String, HashMap<String, HashMap<String, Double>>>>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0).get("http://localhost:9000/").get("POST");
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.keySet(), Matchers.containsInAnyOrder(Matchers.equalTo("minLatency"), Matchers.equalTo("throughput"), Matchers.equalTo("maxLatency"), Matchers.equalTo("avgLatency")));

    }

    private Set<String> prepareClient(final String topic) throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        publisher = new MqttClient(PropertiesReader.getMqttHost(),publisherId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);
        val message = new HashSet<String>();
        publisher.subscribe(topic, (s, mqttMessage) -> {
            //hamcrest can't handle empty sets in the list for contains, so filter them out
            if(s.equals(topic) && !new String(mqttMessage.getPayload()).equals("{}") && !new String(mqttMessage.getPayload()).isEmpty()) {
                log.info("Received "+new String(mqttMessage.getPayload()));
                message.add(new String(mqttMessage.getPayload()));
            }
        });
        return message;
    }
    @AfterEach
    public void closePublisher() throws MqttException, InterruptedException {
        //hack to remove all stored messages
        MqttMessage msg = new MqttMessage(new byte[0]);
        msg.setRetained(true);
        publisher.publish(AssertionStorage.MQTT_TOPIC, msg);
        publisher.publish(TimeStorage.MQTT_TOPIC, msg);
        AssertionStorage.getInstance().reset();
        publisher.disconnect();
        publisher.close();
    }
    private Collection<Map<String, Pair<Integer, Set>>> readResponse(Set<String> messages) throws IOException {
        LinkedList<Map<String, Pair<Integer, Set>>> response = new LinkedList<>();

        //magic to get jackson to serialize to the correct class
        TypeReference<HashMap<String, Pair<Integer, Set<String>>>> typeRef = new TypeReference<>() {};
        for(val item : messages){
            try {
                response.add(mapper.readValue(item, typeRef));
            }
            catch (Exception e){
                log.error(e);
            }
        }
        return response;
    }
    @Test
    public void ResponseCodeAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        val message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(new Atom[0]);
        getWithAuth.run(params);
        Thread.sleep(3000);

        MatcherAssert.assertThat(readResponse(message), Matchers.hasItem(Matchers.hasKey("auth does not return 401")));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("401");
        MatcherAssert.assertThat(readResponse(message), Matchers.contains(Matchers.hasEntry("auth does not return 401", new Pair<>(1, actuals))));
    }
    @Test
    public void ContentTypeAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        val postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[1];
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(new Atom[0]);
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        //simulate failure
        assertion.setContentType("application/xml");
        postWithBodyAndAssertion.run(params);
        Thread.sleep(3000);

        MatcherAssert.assertThat(readResponse(message), Matchers.contains(Matchers.hasKey("postWithBody returns JSON")));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("application/json");
        MatcherAssert.assertThat(readResponse(message), Matchers.contains(Matchers.hasEntry("postWithBody returns JSON", new Pair<>(1, actuals))));
    }

    @Test
    public void AssertionStorageIsDeletedEverySecond() throws MqttException, InterruptedException, ExecutionException, IOException {
        val message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        val postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[1];
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(new Atom[0]);
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        //simulate failure
        assertion.setContentType("application/xml");
        postWithBodyAndAssertion.run(params);
        Thread.sleep(3000);
        MatcherAssert.assertThat(readResponse(message), Matchers.contains(Matchers.hasKey("postWithBody returns JSON")));
        //remove existing values
        message.clear();
        assertion.setContentType("application/json");
        postWithBodyAndAssertion.run(params);
        Thread.sleep(3000);
        //other failure should be removed now
        HashSet<String> actuals = new HashSet<>();
        actuals.add("application/json");
        //empty values are filtered
        MatcherAssert.assertThat(readResponse(message), Matchers.emptyIterable());
    }

    @Test
    public void ResponseNotEmptyAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        val message = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(new Atom[0]);
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        Thread.sleep(3000);

        MatcherAssert.assertThat(readResponse(message), Matchers.hasItem(Matchers.hasKey("jsonObject returns something")));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("");
        MatcherAssert.assertThat(readResponse(message), Matchers.contains(Matchers.hasEntry("jsonObject returns something", new Pair<>(1, actuals))));
    }
}
