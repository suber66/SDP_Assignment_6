package observer;

import java.util.HashMap;
import java.util.Map;

public class StatisticsTracker implements Observer<SensorEvent> {
    private final Map<EventType, Integer> counts = new HashMap<>();

    public StatisticsTracker() {
        for (EventType type : EventType.values()) {
            counts.put(type, 0);
        }
    }

    @Override
    public void update(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!event.getType().isSensorEvent()) {
            return;
        }

        synchronized (counts) {
            int count = counts.get(event.getType());
            counts.put(event.getType(), count + 1);
        }
    }

    public int getCount(EventType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        synchronized (counts) {
            return counts.get(type);
        }
    }

    public int getTotalCount() {
        synchronized (counts) {
            int total = 0;
            for (int count : counts.values()) {
                total += count;
            }
            return total;
        }
    }
}
