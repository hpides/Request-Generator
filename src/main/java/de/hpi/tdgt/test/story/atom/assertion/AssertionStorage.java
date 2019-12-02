package de.hpi.tdgt.test.story.atom.assertion;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Log4j2
public class AssertionStorage {
    private AssertionStorage(){}
    @Getter
    public static final AssertionStorage instance = new AssertionStorage();

    private final Map<String, Integer> fails = new HashMap<>();
    private final Map<String, Set<String>> actuals = new ConcurrentHashMap<>();

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
    public void addActual(String assertionName, String value){
        this.actuals.putIfAbsent(assertionName, new ConcurrentSkipListSet<>());
        val actuals = this.actuals.get(assertionName);
        if (actuals != null) {
            actuals.add(value);
        }
    }
    public Set<String> getActual(String assertionName){
        return this.actuals.getOrDefault(assertionName, new ConcurrentSkipListSet<>());
    }
}
