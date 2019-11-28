package de.hpi.tdgt.test.time_measurement;

import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Log4j2
public class TimeStorage {
    protected TimeStorage(){

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

    public void printSummary(){
        for(val entry : registeredTimes.entrySet()){
            for(val verbMap : entry.getValue().entrySet()) {
                log.info("Endpoint " +verbMap.getKey()+ " " +entry.getKey()+ " min: " + getMin(verbMap.getKey(), entry.getKey())+" ns, max: "+getMax(verbMap.getKey(), entry.getKey())+" ns, avg: "+getAvg(verbMap.getKey(), entry.getKey())+" ns.");
            }
        }
    }


}
