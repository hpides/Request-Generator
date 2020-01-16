package de.hpi.tdgt.test.story.atom.assertion;

import de.hpi.tdgt.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MqttAssertionMessage {
    private long testId;
    private Map<String, Pair<Integer, Set<String>>> actuals;
}
