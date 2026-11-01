package observer;

import java.time.LocalDateTime;

public final class SensorEvent {
    private final EventType type;
    private final String sourceId;
    private final String districtId;
    private final LocalDateTime timestamp;
    private final String message;

    public SensorEvent(EventType type, String sourceId, String districtId, LocalDateTime timestamp, String message) {
        if (type == null || sourceId == null || districtId == null || timestamp == null || message == null) {
            throw new IllegalArgumentException("Event fields must not be null");
        }
        this.type = type;
        this.sourceId = sourceId;
        this.districtId = districtId;
        this.timestamp = timestamp;
        this.message = message;
    }

    public EventType getType() {
        return type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
