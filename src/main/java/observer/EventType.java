package observer;

public enum EventType {
    AIR_POLLUTION_HIGH,
    TRAFFIC_JAM,
    WATER_LEVEL_CRITICAL,
    SENSOR_OFFLINE,
    DISTRICT_SUMMARY,
    INSPECTION_COMPLETED;

    public boolean isSensorEvent() {
        return this == AIR_POLLUTION_HIGH
                || this == TRAFFIC_JAM
                || this == WATER_LEVEL_CRITICAL
                || this == SENSOR_OFFLINE;
    }
}
