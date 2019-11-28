package de.hpi.tdgt.test.story.activity.assertion;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class AssertionStorage {
    private AssertionStorage(){}
    @Getter
    public static final AssertionStorage instance = new AssertionStorage();

    private final Map<String, Integer> fails = new HashMap<>();

    public int getFails(String assertionName){
        return fails.getOrDefault(assertionName, 0);
    }

    public synchronized void addFailure(String assertionName){
        int current = fails.getOrDefault(assertionName, 0);
        current++;
        fails.put(assertionName,current);
    }

    public void reset(){
        fails.clear();
    }
}
