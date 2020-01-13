package de.hpi.tdgt.test.time_measurement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@NoArgsConstructor
@Getter
@Setter
public class MqttTimeMessage {
    private Map<String, Map<String, Map<String, Map<String, String>>>> times = new ConcurrentHashMap<>();
    private long testId;
    private long creationTime;
}
