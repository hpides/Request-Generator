package de.hpi.tdgt.Stats;

import com.lmax.disruptor.EventHandler;


public class TotalStatistic implements EventHandler<StatisticEvent> {

    private final Statistic stats = new Statistic();



    @Override
    public void onEvent(StatisticEvent statisticEvent, long sequence, boolean endOfBatch) throws Exception {
        stats.Merge(statisticEvent);
        System.out.println(stats.ToPercentileString());
    }
}
