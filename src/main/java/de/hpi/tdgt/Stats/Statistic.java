package de.hpi.tdgt.Stats;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static de.hpi.tdgt.Stats.Population.NONE;


public class Statistic {
    Population total;
    Map<Endpoint, Population> populations;
    TIntObjectMap<ErrorEntry> errors;
    public long id;

    public Statistic(long id) throws MalformedURLException {
        total = new Population(this, new Endpoint(new URL("http://total"), "GET"));
        this.id = id;
        populations = new HashMap<>();
        errors = new TIntObjectHashMap<>();
    }

    public void AddSample(Endpoint ep, long responseTime, int contentLength) {
        total.AddSample(responseTime, contentLength);
        Population p = populations.get(ep);
        if (p == null) {
            p = new Population(this, ep);
            populations.put(ep, p);
        }
        p.AddSample(responseTime, contentLength);
    }

    public void AddError(Endpoint ep, String error) {
        total.AddError();
        Population p = populations.get(ep);
        if (p == null) {
            p = new Population(this, ep);
            populations.put(ep, p);
        }
        p.AddError();

        int key = ErrorEntry.GetHashCode(ep, error);
        ErrorEntry entry = errors.get(key);
        if (entry == null) {
            entry = new ErrorEntry(ep, error);
            errors.put(key, entry);
        } else {
            entry.Occured();
        }
    }


    public void Merge(StatisticEvent otherStat) {
        for (CompressedPopulation p : otherStat.populations) {
            Population existingPop = populations.get(p.endpoint);
            if (existingPop == null) {
                existingPop = new Population(this, p.endpoint);
                populations.put(p.endpoint, existingPop);
            }
            existingPop.Merge(p);
        }

        otherStat.errors.forEachEntry(new TIntObjectProcedure<ErrorEntry>() {
            @Override
            public boolean execute(int i, ErrorEntry errorEntry) {
                ErrorEntry existingEntry = errors.get(i);
                if (existingEntry == null)
                    errors.put(i, errorEntry);
                else
                    existingEntry.Merge(errorEntry);
                return true;
            }
        });

        long oldLastRequestTimeStamp = total.LastRequestTimeStamp();
        if (oldLastRequestTimeStamp == NONE)
            oldLastRequestTimeStamp = 0;

        total.Merge(otherStat.total);

        if (total.LastRequestTimeStamp() != NONE && total.LastRequestTimeStamp() > oldLastRequestTimeStamp) {
            total.CacheResponseTime(total.LastRequestTimeStamp());
        }
    }

    public long LastRequestTimeStamp() {
        return total.LastRequestTimeStamp();
    }

    public long StartTime() {
        return total.StartTime();
    }

    public void Clear() {
        errors.clear();
        total.Reset();
        populations.forEach((e, p) -> p.Reset());
    }

    public boolean IsEmpty() {
        return errors.size() == 0 && total.IsEmpty();
    }

    public StatisticProtos.Statistic Serialize(){
        StatisticProtos.Statistic.Builder builder = StatisticProtos.Statistic.newBuilder();
        builder.setTotal(total.Serialize());
        builder.setId(this.id);
        populations.values().forEach((val) -> {builder.addPopulations(val.Serialize());});
        errors.forEachValue((val) -> {
            builder.addErrors(val.Serialize());
            return true;
        });
        return builder.build();
    }

    @Override
    public String toString() {
        if (IsEmpty())
            return "Empty";

        StringBuilder builder = new StringBuilder(300);
        builder.append(String.format(" %-" + 60 + "s %7s %12s %7s %7s %7s  | %7s %7s %7s", "Name", "# reqs", "# fails", "Avg", "Min", "Max", "Median", "req/s", "failures/s"));
        builder.append(System.lineSeparator());
        builder.append("-".repeat(80 + 60));
        builder.append(System.lineSeparator());

        Endpoint[] sortedKeys = populations.keySet().toArray(Endpoint[]::new);
        Arrays.sort(sortedKeys);

        for (Endpoint key : sortedKeys) {
            Population p = populations.get(key);
            if (!p.IsEmpty()) {
                builder.append(p.toString());
                builder.append(System.lineSeparator());
            }
        }
        builder.append("-".repeat(80 + 60));
        builder.append(System.lineSeparator());
        builder.append(total.toString());
        builder.append(System.lineSeparator());
        builder.append(ErrorReportString());
        return builder.toString();
    }

    public String ToPercentileString() {
        StringBuilder builder = new StringBuilder(300);
        builder.append(String.format(" %-20s %-60s %8s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s",
                "Type",
                "Name",
                "# reqs",
                "50%",
                "66%",
                "75%",
                "80%",
                "90%",
                "95%",
                "98%",
                "99%",
                "99.9%",
                "99.99%",
                "100%"));
        builder.append('\n');
        builder.append("-".repeat(90 + 60));
        builder.append('\n');

        Endpoint[] sortedKeys = populations.keySet().toArray(Endpoint[]::new);
        Arrays.sort(sortedKeys);

        for (Endpoint key : sortedKeys) {
            Population p = populations.get(key);
            if (p.LastRequestTimeStamp() != NONE) {
                builder.append(p.ResponseTimePercentileString());
            }
            builder.append('\n');
        }
        builder.append("-".repeat(90 + 60));
        builder.append('\n');
        builder.append(total.ResponseTimePercentileString());
        builder.append('\n');
        return builder.toString();
    }

    public String ErrorReportString() {
        StringBuilder builder = new StringBuilder(300);
        builder.append("Error report");
        builder.append(System.lineSeparator());
        builder.append(String.format(" %-18s %-100s", "# occurences", "Error"));
        builder.append(System.lineSeparator());
        builder.append("-".repeat(80 + 60));
        builder.append(System.lineSeparator());
        errors.forEachValue(e -> {
            builder.append(String.format(" %-18d %-100s", e.occurences, e.error));
            builder.append('\n');
            return true;
        });
        builder.append("-".repeat(80 + 60));
        builder.append(System.lineSeparator());
        return builder.toString();
    }
}
