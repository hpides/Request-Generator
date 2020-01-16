package de.hpi.tdgt.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.UserStory;
import de.hpi.tdgt.test.story.atom.Atom;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.story.atom.assertion.ContentType;
import de.hpi.tdgt.test.story.atom.assertion.MqttAssertionMessage;
import de.hpi.tdgt.test.time_measurement.MqttTimeMessage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import de.hpi.tdgt.util.Pair;
import de.hpi.tdgt.util.PropertiesReader;
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
import java.util.concurrent.ExecutionException;

@Log4j2
public class MQTTTest extends RequestHandlingFramework {
    private ObjectMapper mapper = new ObjectMapper();
    private IMqttClient publisher;
    private final UserStory mockParent = new UserStory();
    private final String mockStoryName = "StoryName";
    @BeforeEach
    public void beforeEach(){
        //this test MUST handle asynch behaviour
        AssertionStorage.getInstance().setStoreEntriesAsynch(true);
        TimeStorage.getInstance().setStoreEntriesAsynch(true);
        //scenario that a message we want and a message we don't want arrive at the same time is prevented
        TimeStorage.getInstance().setSendOnlyNonEmpty(true);
        val mockTest = new de.hpi.tdgt.test.Test();
        mockParent.setParent(mockTest);
        mockParent.setName(mockStoryName);
    }
    @Test
    public void TimeStorageStreamsTimesUsingMQTT() throws MqttException, InterruptedException, IOException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10, "story",0);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }

        MatcherAssert.assertThat(response.get(0).getTimes(), Matchers.hasKey("http://localhost:9000/"));

           }

    @Test
    public void TimeStorageStreamsAllTimesUsingMQTT() throws MqttException, InterruptedException, IOException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        String storyName = "story";
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10, storyName,0);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0).getTimes().get("http://localhost:9000/").get("POST").get(storyName);
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.keySet(), Matchers.containsInAnyOrder(Matchers.equalTo("minLatency"), Matchers.equalTo("throughput"), Matchers.equalTo("maxLatency"), Matchers.equalTo("avgLatency")));

    }
    @Test
    public void TimeStorageStreamsAllTimesOfAllStoriesUsingMQTT() throws MqttException, InterruptedException, IOException {
        //this test is based on the assumption that both entries are added at roughly the same time, so we want predictable timing behavior
        TimeStorage.getInstance().setStoreEntriesAsynch(false);
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        String storyName1 = "story1";
        String storyName2 = "story2";
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 10, storyName1,0);
        TimeStorage.getInstance().registerTime("POST","http://localhost:9000/", 20, storyName2,0);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        MatcherAssert.assertThat("We should have 2 story entries for \"story1\" and \"story2\"", response.get(0).getTimes().get("http://localhost:9000/").get("POST").size(), Matchers.is(2));
        val times1 = response.get(0).getTimes().get("http://localhost:9000/").get("POST").get(storyName1);
        val times2 = response.get(0).getTimes().get("http://localhost:9000/").get("POST").get(storyName2);
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times1.keySet(), Matchers.containsInAnyOrder(Matchers.equalTo("minLatency"), Matchers.equalTo("throughput"), Matchers.equalTo("maxLatency"), Matchers.equalTo("avgLatency")));
        MatcherAssert.assertThat(times2.keySet(), Matchers.containsInAnyOrder(Matchers.equalTo("minLatency"), Matchers.equalTo("throughput"), Matchers.equalTo("maxLatency"), Matchers.equalTo("avgLatency")));
        MatcherAssert.assertThat(times1, Matchers.hasEntry("maxLatency", "10"));
        MatcherAssert.assertThat(times2, Matchers.hasEntry("maxLatency", "20"));

    }
    @Test
    public void TimeStorageStreamsAllTimesUsingMQTTWithCorrectStoryName() throws MqttException, InterruptedException, IOException, ExecutionException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(new Atom[0]);
        val name = mockStoryName;
        getWithAuth.setParent(mockParent);
        getWithAuth.run(params);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0).getTimes().get("http://localhost:9000/auth").get("GET");
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times, Matchers.hasKey( name));
    }
    @Test
    public void TimeStorageStreamsAllTimesUsingMQTTWithCorrectThroughput() throws MqttException, InterruptedException, IOException, ExecutionException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(new Atom[0]);
        val name = mockStoryName;
        getWithAuth.setParent(mockParent);
        getWithAuth.run(params);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0).getTimes().get("http://localhost:9000/auth").get("GET").get(name);
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times, Matchers.hasEntry("throughput", "1"));
    }

    @Test
    public void TimeStorageStreamsAllTimesUsingMQTTWithATestId() throws MqttException, InterruptedException, IOException, ExecutionException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(new Atom[0]);
        val name = mockStoryName;
        getWithAuth.setParent(mockParent);
        getWithAuth.run(params);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0);
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.getTestId(), Matchers.greaterThan(0L));
    }

    @Test
    public void TimeStorageStreamsAllTimesUsingMQTTWithACreationTime() throws MqttException, InterruptedException, IOException, ExecutionException {
        val messages = prepareClient(TimeStorage.MQTT_TOPIC);
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        val getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(new Atom[0]);
        val name = mockStoryName;
        getWithAuth.setParent(mockParent);
        getWithAuth.run(params);
        Thread.sleep(3000);
        TypeReference<MqttTimeMessage> typeRef = new TypeReference<>() {};
        val response = new Vector<MqttTimeMessage>();
        for(val item : messages){
            response.add(mapper.readValue(item, typeRef));
        }
        val times = response.get(0);
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.getCreationTime(), Matchers.greaterThan(0L));
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
    private LinkedList<MqttAssertionMessage> readAssertion(Set<String> messages) throws IOException {
        LinkedList<MqttAssertionMessage> response = new LinkedList<>();

        //magic to get jackson to serialize to the correct class
        TypeReference<MqttAssertionMessage> typeRef = new TypeReference<>() {};
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
        val allActuals = getAllActuals(message);
        MatcherAssert.assertThat(allActuals, Matchers.hasItem(Matchers.hasKey("auth does not return 401")));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("401");
        MatcherAssert.assertThat(allActuals.get(allActuals.size()-1), Matchers.hasEntry("auth does not return 401", new Pair<>(1, actuals)));
    }
    @Test
    public void ContentTypeAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC);
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
        val allActuals = getAllActuals(messages);
        MatcherAssert.assertThat(allActuals.get(allActuals.size()-1), Matchers.hasKey("postWithBody returns JSON"));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("application/json");
        MatcherAssert.assertThat(allActuals.get(allActuals.size()-1), Matchers.hasEntry("postWithBody returns JSON", new Pair<>(1, actuals)));
    }

    private LinkedList<Map<String, Pair<Integer, Set<String>>>> getAllActuals(Set<String> messages) throws IOException {
        val allActuals = new LinkedList<Map<String, Pair<Integer, Set<String>>>>();
        for(val message : readAssertion(messages)){
            if(!message.getActuals().isEmpty()) {
                allActuals.add(message.getActuals());
            }
        }
        return allActuals;
    }

    @Test
    public void AssertionStorageIsDeletedEverySecond() throws MqttException, InterruptedException, ExecutionException, IOException {
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC);
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
        var allActuals = getAllActuals(messages);
        MatcherAssert.assertThat(allActuals.get(allActuals.size()-1), Matchers.hasKey("postWithBody returns JSON"));
        //remove existing values
        messages.clear();
        assertion.setContentType("application/json");
        postWithBodyAndAssertion.run(params);
        Thread.sleep(3000);
        //other failure should be removed now
        HashSet<String> actuals = new HashSet<>();
        actuals.add("application/json");
        allActuals = getAllActuals(messages);
        //empty values are filtered
        MatcherAssert.assertThat(allActuals, Matchers.emptyIterable());
    }

    @Test
    public void ResponseNotEmptyAssertionStreamsFailedAssertions() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(new Atom[0]);
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        Thread.sleep(3000);
        val allActuals = getAllActuals(messages);
        MatcherAssert.assertThat(allActuals, Matchers.hasItem(Matchers.hasKey("jsonObject returns something")));
        HashSet<String> actuals = new HashSet<>();
        actuals.add("");
        //hamcrest Matchers.contains did not work, so assume the wanted entry is the last
        MatcherAssert.assertThat(allActuals.get(allActuals.size()-1), Matchers.hasEntry("jsonObject returns something", new Pair<>(1, actuals)));
    }
    @Test
    public void ResponseNotEmptyAssertionStreamsFailedAssertionsWithTestId() throws MqttException, InterruptedException, ExecutionException, IOException {
        val params = new HashMap<String, String>();
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC);
        val getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(new Atom[0]);
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        Thread.sleep(3000);
        val actuals = readAssertion(messages);
        MatcherAssert.assertThat(actuals.get(0).getTestId(), Matchers.greaterThan(0L));
    }

    @Test
    public void ATestStartMessageIsSent() throws MqttException, InterruptedException, ExecutionException, IOException {
        val message = prepareClient(de.hpi.tdgt.test.Test.MQTT_TOPIC);
        //test that does not do anything is sufficient, no need to waste resources here
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getNoopJson());
        test.start(test.warmup());
        String messageStart = "testStart";
        boolean hasTestStart = hasMessageStartingWith(message, messageStart);
        MatcherAssert.assertThat("control topic should have received a \"testStart\"!",hasTestStart);
    }

    @Test
    public void ATestEndMessageIsSent() throws MqttException, InterruptedException, ExecutionException, IOException {
        val messages = prepareClient(de.hpi.tdgt.test.Test.MQTT_TOPIC);
        //test that does not do anything is sufficient, no need to waste resources here
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getNoopJson());
        test.start(test.warmup());
        String messageEnd = "testEnd";
        boolean hasTestEnd = hasMessageStartingWith(messages, messageEnd);
        MatcherAssert.assertThat("control topic should have received a \"testEnd\"!",hasTestEnd);
    }

    private boolean hasMessageStartingWith(Set<String> messages, String messageStart) {
        boolean hasTestEnd = false;
        for(val message : messages){
            if (message.startsWith(messageStart)){
                hasTestEnd = true;
            }
        }
        return hasTestEnd;
    }
}
