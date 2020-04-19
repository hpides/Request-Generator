package de.hpi.tdgt.Stats;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class StatisticEvent {

    public CompressedPopulation total;
    public CompressedPopulation[] populations;
    public TIntObjectMap<ErrorEntry> errors;

    public void Set(Statistic statistic)
    {
        this.populations = statistic.populations.values().stream().map(
                (Population p) -> p.Compress())
                .toArray(CompressedPopulation[]::new);
        this.errors = new TIntObjectHashMap<>(statistic.errors);
        this.errors.transformValues((ErrorEntry e) -> new ErrorEntry(e));
        this.total = statistic.total.Compress();
    }

    @Override
    public String toString() {
        return total.toString();
    }
}
