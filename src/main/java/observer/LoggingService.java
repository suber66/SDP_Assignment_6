package observer;

import java.util.ArrayList;
import java.util.List;

public class LoggingService implements Observer<SensorEvent> {
    private final List<SensorEvent> events = new ArrayList<>();

    @Override
    public void update(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        synchronized (events) {
            events.add(event);
        }
    }

    public List<SensorEvent> getEvents() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }
}
