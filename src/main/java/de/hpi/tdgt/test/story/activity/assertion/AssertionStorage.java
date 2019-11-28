package de.hpi.tdgt.test.story.activity.assertion;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
@Log4j2
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

    public void printSummary(){
        for(val entry :fails.entrySet()){
            log.info("Assertion "+entry.getKey()+" failed "+entry.getValue()+" times.");
        }
    }
}
