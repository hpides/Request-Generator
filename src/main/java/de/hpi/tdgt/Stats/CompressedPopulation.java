package de.hpi.tdgt.Stats;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

public class CompressedPopulation {

    public final  Endpoint endpoint;

    public final  int numRequests;
    public final  int numFailures;
    public final  long totalResponseTime;
    public final  long minResponseTime;
    public final  long maxResponseTime;
    public final  TLongIntMap numRequestsPerSecond;
    public final  TLongIntMap failuresPerSecond;

    public final  int totalContentLength;
    public final  long startTime;
    public final  long latestRequestTime;

    public final  TLongIntMap responseTimes;

    public CompressedPopulation(Endpoint endpoint,
                                int numRequests,
                                int numFailures,
                                long totalResponseTime,
                                long minResponseTime,
                                long maxResponseTime,
                                TLongIntMap numRequestsPerSecond,
                                TLongIntMap failuresPerSecond,
                                int totalContentLength,
                                long startTime,
                                long latestRequestTime,
                                TLongIntMap responseTimes) {

        this.endpoint = endpoint;
        this.numRequests = numRequests;
        this.numFailures = numFailures;
        this.totalResponseTime = totalResponseTime;
        this.minResponseTime = minResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.numRequestsPerSecond = new TLongIntHashMap(numRequestsPerSecond);
        this.failuresPerSecond = new TLongIntHashMap(failuresPerSecond);
        this.totalContentLength = totalContentLength;
        this.startTime = startTime;
        this.latestRequestTime = latestRequestTime;
        this.responseTimes = new TLongIntHashMap(responseTimes);
    }
}
