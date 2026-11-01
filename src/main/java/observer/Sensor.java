package observer;

import java.time.LocalDateTime;

public class Sensor extends AbstractSubject {
    private final String id;
    private final String districtId;
    private final EventType type;
    private final double threshold;
    private final String unit;

    public Sensor(String id, String districtId, EventType type, double threshold, String unit) {
        if (id == null || districtId == null || type == null || unit == null) {
            throw new IllegalArgumentException("Sensor fields must not be null");
        }
        if (!type.isSensorEvent() || type == EventType.SENSOR_OFFLINE) {
            throw new IllegalArgumentException("Sensor needs a measurement event type");
        }
        if (!Double.isFinite(threshold) || threshold < 0.0) {
            throw new IllegalArgumentException("threshold must be finite and non-negative");
        }
        this.id = id;
        this.districtId = districtId;
        this.type = type;
        this.threshold = threshold;
        this.unit = unit;
    }

    public void measure(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("Reading must be finite and non-negative");
        }
        if (value > threshold) {
            publish(type, "Critical reading: " + value + " " + unit);
        }
    }

    public void markOffline() {
        publish(EventType.SENSOR_OFFLINE, "Sensor is offline");
    }

    private void publish(EventType eventType, String message) {
        SensorEvent event = new SensorEvent(eventType, id, districtId, LocalDateTime.now(), message);
        notifyListeners(event);
    }
}
