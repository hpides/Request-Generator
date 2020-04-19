package de.hpi.tdgt.Stats;

import com.lmax.disruptor.RingBuffer;

public class StatisticPublisher {

    public final Statistic stats = new Statistic();
    private final RingBuffer<StatisticEvent> ringBuffer;

    private long lastUpdateTime = 0;

    public StatisticPublisher(RingBuffer<StatisticEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void Update()
    {
        long t = System.currentTimeMillis();
        if(t - lastUpdateTime > 1000)
        {
            lastUpdateTime = t;
            ringBuffer.publishEvent(StatisticPublisher::translate, stats);
        }
    }

    public static void translate(StatisticEvent event, long sequence, Statistic stats)
    {
        event.Set(stats);
    }
}
