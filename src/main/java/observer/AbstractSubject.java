package observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractSubject implements Subject<SensorEvent> {
    private final Map<EventType, List<Observer<SensorEvent>>> registry = new HashMap<>();

    protected AbstractSubject() {
        for (EventType type : EventType.values()) {
            registry.put(type, new ArrayList<>());
        }
    }

    @Override
    public void subscribe(EventType type, Observer<SensorEvent> observer) {
        if (type == null || observer == null) {
            throw new IllegalArgumentException("type and observer must not be null");
        }

        synchronized (registry) {
            List<Observer<SensorEvent>> observers = registry.get(type);
            if (!observers.contains(observer)) {
                observers.add(observer);
            }
        }
    }

    @Override
    public void unsubscribe(EventType type, Observer<SensorEvent> observer) {
        if (type == null || observer == null) {
            throw new IllegalArgumentException("type and observer must not be null");
        }

        synchronized (registry) {
            registry.get(type).remove(observer);
        }
    }

    @Override
    public void notifyListeners(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        List<Observer<SensorEvent>> observers;
        synchronized (registry) {
            observers = new ArrayList<>(registry.get(event.getType()));
        }

        for (Observer<SensorEvent> observer : observers) {
            try {
                observer.update(event);
            } catch (RuntimeException exception) {
                System.err.println("Observer failed for event " + event.getType() + ": " + exception.getMessage());
            }
        }
    }
}
