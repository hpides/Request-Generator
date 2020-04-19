package de.hpi.tdgt.Stats;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TLongIntProcedure;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Arrays;

import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;

public class Population {

    private static long RoundTime(long time) {
        if (time < 10) {
            // 3 -> 3
            return time;
        } else if (time < 100) {
            //33 -> 30
            return Math.round(time / 10.0) * 10L;
        } else if (time < 1000) {
            // 543 -> 500
            return Math.round(time / 100.0) * 100L;
        } else if (time < 10000) {
            // 1235 -> 1000
            return Math.round(time / 1000.0) * 1000L;
        } else {
            // 63235 -> 60000
            return Math.round(time / 10000.0) * 10000L;
        }
    }

    /// percentile = percentile to calculate. Between 0.0 - 1.0
    private static long CalculateResponseTimePercentile(TLongIntMap responseTimes, int numRequests, float percentile) {
        int percentileRequestCount = (int) (numRequests * percentile);

        long[] sortedKeys = responseTimes.keys();
        Arrays.sort(sortedKeys);

        int processedCount = 0;
        for (int index = sortedKeys.length - 1; index >= 0; index--) {
            int value = responseTimes.get(sortedKeys[index]);
            processedCount += value;
            if (numRequests - processedCount <= percentileRequestCount) {
                return sortedKeys[index];
            }
        }
        return 0;
    }

    private static TLongIntMap DiffResponseTimeDicts(TLongIntMap latest, TLongIntMap old) {
        TLongIntMap result = new TLongIntHashMap(latest.size());
        latest.forEachEntry(new TLongIntProcedure() {
            @Override
            public boolean execute(long l, int i) {
                int diff = i - old.get(l);
                if (diff != 0)
                    result.put(l, diff);
                return true;
            }
        });
        return result;
    }

    private static final int CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW = 10;

    public static final long NONE = -1;

    private final Endpoint endpoint;

    private int numRequests;
    private int numFailures;
    private long totalResponseTime;
    private long minResponseTime;
    private long maxResponseTime;
    private final TLongIntMap numRequestsPerSecond;
    private final TLongIntMap failuresPerSecond;

    private int totalContentLength;
    private long startTime;
    private long latestRequestTime;

    private final TLongIntMap responseTimes;
    private final CircularFifoQueue responseTimeCache;

    private Statistic statistic;

    public Population(Statistic statistic, Endpoint endpoint) {
        this.statistic = statistic;
        this.endpoint = endpoint;

        numRequestsPerSecond = new TLongIntHashMap(100, DEFAULT_LOAD_FACTOR, 0, 0);
        failuresPerSecond = new TLongIntHashMap(100);
        responseTimes = new TLongIntHashMap(300);
        responseTimeCache = new CircularFifoQueue(CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW + 10);

        Reset();
    }

    public void Reset() {
        numRequests = 0;
        numFailures = 0;
        totalResponseTime = 0;
        minResponseTime = Long.MAX_VALUE;
        maxResponseTime = Long.MIN_VALUE;

        totalContentLength = 0;
        startTime = System.currentTimeMillis() / 1000;
        latestRequestTime = NONE;

        numRequestsPerSecond.clear();
        failuresPerSecond.clear();
        responseTimes.clear();
        responseTimeCache.clear();
    }

    // responseTime in ms
    // time in seconds
    public void AddSample(long responseTime, int contentLength) {
        long time = System.currentTimeMillis() / 1000;

        if (time > latestRequestTime && latestRequestTime != NONE)
            CacheResponseTime(latestRequestTime - 1);

        ++numRequests;
        totalContentLength += contentLength;

        HandleTimeOfRequest(time);
        HandleResponseTime(responseTime);
    }

    public void AddError() {
        long time = System.currentTimeMillis() / 1000;
        ++numFailures;
        failuresPerSecond.adjustOrPutValue(time, 1, 1);
    }

    public float FailRatio() {
        if (numRequests == 0) {
            if (numFailures == 0)
                return 0;
            else
                return 1;
        } else {
            return (float) numFailures / (float) numRequests;
        }
    }

    public boolean IsEmpty()
    {
        return numRequests == 0;
    }


    public long AvgResponseTime() {
        if(numRequests == 0)
            return 0;
        return totalResponseTime / numRequests;
    }

    public float CurrentRequestsPerSecond() {
        if (statistic.LastRequestTimeStamp() == NONE)
            return 0;

        // average of last 12 seconds
        long sliceStart = Math.max(statistic.LastRequestTimeStamp() - 12, statistic.StartTime());
        int total = 0;
        for (long i = sliceStart; i < statistic.LastRequestTimeStamp() - 2; i++) {
            total += numRequestsPerSecond.get(i);
        }

        float count = ((statistic.LastRequestTimeStamp() - 2) - sliceStart);
        if (count <= 0)
            count = 1;

        return (float) total / count;
    }

    public float CurrentFailPerSecond() {
        if (statistic.LastRequestTimeStamp() == NONE)
            return 0;

        // average of last 12 seconds
        long sliceStart = Math.max(statistic.LastRequestTimeStamp() - 12, statistic.StartTime());
        int total = 0;
        for (long i = sliceStart; i < statistic.LastRequestTimeStamp() - 2; i++) {
            total += failuresPerSecond.get(i);
        }

        float count = ((statistic.LastRequestTimeStamp() - 2) - sliceStart);
        if (count <= 0)
            count = 1;

        return (float) total / count;
    }

    public float TotalRequestsPerSecond() {
        if (statistic.LastRequestTimeStamp() == NONE)
            return 0;

        long runtime = (statistic.LastRequestTimeStamp() - statistic.StartTime());
        if (runtime == 0)
            return 0;
        return (float) numRequests / (float) runtime;
    }

    public float TotalFailuresPerSecond() {
        if (statistic.LastRequestTimeStamp() == NONE)
            return 0;

        long runtime = (statistic.LastRequestTimeStamp() - statistic.StartTime());
        if (runtime == 0)
            return 0;
        return (float) numFailures / (float) runtime;
    }

    public float AverageContentLength() {
        return (float) totalContentLength / (float) numRequests;
    }

    public void Merge(CompressedPopulation other) {
        /*
        Extend the data from the current StatsEntry with the stats from another
        StatsEntry instance.
        */

        if (latestRequestTime != NONE && other.latestRequestTime != NONE)
            latestRequestTime = Math.max(latestRequestTime, other.latestRequestTime);
        else if (other.latestRequestTime != NONE)
            latestRequestTime = other.latestRequestTime;

        startTime = Math.min(startTime, other.startTime);

        numRequests = numRequests + other.numRequests;
        numFailures = numFailures + other.numFailures;
        totalResponseTime = totalResponseTime + other.totalResponseTime;
        maxResponseTime = Math.max(maxResponseTime, other.maxResponseTime);
        minResponseTime = Math.min(minResponseTime, other.minResponseTime);
        totalContentLength = totalContentLength + other.totalContentLength;

        other.responseTimes.forEachEntry(
                new TLongIntProcedure() {
                    @Override
                    public boolean execute(long l, int i) {
                        responseTimes.adjustOrPutValue(l, i, i);
                        return true;
                    }
                }
        );

        other.numRequestsPerSecond.forEachEntry(new TLongIntProcedure() {
            @Override
            public boolean execute(long l, int i) {
                numRequestsPerSecond.adjustOrPutValue(l, i, i);
                return true;
            }
        });

        other.failuresPerSecond.forEachEntry(new TLongIntProcedure() {
            @Override
            public boolean execute(long l, int i) {
                failuresPerSecond.adjustOrPutValue(l, i, i);
                return true;
            }
        });
    }


    /*
        Get the response time that a certain number of percent of the requests
        finished within.

        Percent specified in range: 0.0 - 1.0
    */
    public long GetResponseTimePercentile(float percentile) {
        return CalculateResponseTimePercentile(responseTimes, numRequests, percentile);
    }

    /*
        Calculate the *current* response time for a certain percentile. We use a sliding
        window of (approximately) the last 10 seconds (specified by CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW)
        when calculating this.
     */
    public long GetCurrentResponseTimePercentile(float percentile) {
        long time = System.currentTimeMillis() / 1000;

        long targetTime = time - CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW;
        CachedResponseTime closest = null;
        long smallestTimeDelta = Long.MAX_VALUE;

        for (Object cachedObject : responseTimeCache) {
            CachedResponseTime cached = (CachedResponseTime) cachedObject;
            long delta = Math.abs(cached.cacheTimeInSeconds - targetTime);

            if (delta > smallestTimeDelta) {
                break;
            }
            smallestTimeDelta = delta;
            closest = cached;
        }

        if (closest != null) {
            return CalculateResponseTimePercentile(
                    DiffResponseTimeDicts(responseTimes, closest.responseTimes),
                    numRequests - closest.numRequests,
                    percentile
            );
        }
        return 0;
    }

    public long MedianResponseTime() {
        if (responseTimes.size() == 0)
            return 0;

        int medianPos = (numRequests - 1) / 2;

        long[] sortedKeys = responseTimes.keys();
        long medianTime = 0;
        Arrays.sort(sortedKeys);
        for (long k : sortedKeys) {
            int c = responseTimes.get(k);
            if (medianPos < c) {
                medianTime = k;
                break;
            }
            medianPos -= c;
        }
        if (medianTime > maxResponseTime)
            medianTime = maxResponseTime;
        else if (medianTime < minResponseTime)
            medianTime = minResponseTime;
        return medianTime;
    }

    public long LastRequestTimeStamp() {
        return latestRequestTime;
    }

    public long StartTime() {
        return startTime;
    }

    public CompressedPopulation Compress() {
        CompressedPopulation cp = new CompressedPopulation(
                endpoint,
                numRequests,
                numFailures,
                totalResponseTime,
                minResponseTime,
                maxResponseTime,
                numRequestsPerSecond,
                failuresPerSecond,
                totalContentLength,
                startTime,
                latestRequestTime,
                responseTimes
        );
        Reset();
        return cp;
    }

    @Override
    public String toString() {
        float rps = TotalRequestsPerSecond();
        float failps = TotalFailuresPerSecond();

        return String.format("%-60s %7d %12s %7d %7d %7d  | %7d %7.2f %7.2f",
                endpoint.method + " " + endpoint.url,
                numRequests,
                String.format("%d(%.2f%%)", numFailures, FailRatio() * 100),
                AvgResponseTime(),
                minResponseTime,
                maxResponseTime,
                MedianResponseTime(),
                rps,
                failps
        );
    }

    public String ResponseTimePercentileString() {
        if (numRequests == 0)
            return "Can't calculate percentile on url with no successful requests";

        return String.format(" %-20s %-60s %8d %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d %6d",
                this.endpoint.method,
                this.endpoint.url,
                this.numRequests,
                this.GetResponseTimePercentile(0.5f),
                this.GetResponseTimePercentile(0.66f),
                this.GetResponseTimePercentile(0.75f),
                this.GetResponseTimePercentile(0.80f),
                this.GetResponseTimePercentile(0.90f),
                this.GetResponseTimePercentile(0.95f),
                this.GetResponseTimePercentile(0.98f),
                this.GetResponseTimePercentile(0.99f),
                this.GetResponseTimePercentile(0.999f),
                this.GetResponseTimePercentile(0.9999f),
                this.GetResponseTimePercentile(1.0f)
        );
    }

    private void HandleTimeOfRequest(long time) {
        numRequestsPerSecond.adjustOrPutValue(time, 1, 1);
        latestRequestTime = time;
    }

    private void HandleResponseTime(long responseTime) {
        totalResponseTime += responseTime;

        minResponseTime = Math.min(responseTime, minResponseTime);
        maxResponseTime = Math.max(responseTime, maxResponseTime);

        long roundedResponseTime = RoundTime(responseTime);

        responseTimes.adjustOrPutValue(roundedResponseTime, 1, 1);
    }

    public void CacheResponseTime(long timeInSeconds) {
        responseTimeCache.add(new CachedResponseTime(
                timeInSeconds,
                numRequests,
                new TLongIntHashMap(responseTimes)));
    }

    private final class CachedResponseTime {
        public final int numRequests;
        public final TLongIntMap responseTimes;
        public final long cacheTimeInSeconds;

        public CachedResponseTime(long timeInSeconds, int numRequests, TLongIntMap responseTimes) {
            this.numRequests = numRequests;
            this.responseTimes = responseTimes;
            this.cacheTimeInSeconds = timeInSeconds;
        }
    }
}

