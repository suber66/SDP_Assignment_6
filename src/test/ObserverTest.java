import org.junit.jupiter.api.Test;
import observer.DistrictAggregator;
import observer.EventType;
import observer.LoggingService;
import observer.Observer;
import observer.Sensor;
import observer.SensorEvent;
import observer.StatisticsTracker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverTest {
    @Test
    void routesEventsByTypeAndAvoidsDuplicateSubscriptions() {
        List<Sensor> sensors = Main.createSensors();
        Sensor air = sensors.get(0);
        LoggingService airLog = new LoggingService();
        LoggingService waterLog = new LoggingService();
        air.subscribe(EventType.AIR_POLLUTION_HIGH, event -> {
            throw new IllegalStateException("Test listener failed");
        });
        air.subscribe(EventType.AIR_POLLUTION_HIGH, airLog);
        air.subscribe(EventType.AIR_POLLUTION_HIGH, airLog);
        air.subscribe(EventType.WATER_LEVEL_CRITICAL, waterLog);

        sensors.get(1).measure(6.0);
        assertTrue(airLog.getEvents().isEmpty());
        air.measure(200.0);

        assertEquals(1, airLog.getEvents().size());
        assertTrue(waterLog.getEvents().isEmpty());
        SensorEvent event = airLog.getEvents().get(0);
        assertEquals(EventType.AIR_POLLUTION_HIGH, event.getType());
        assertEquals("air-1", event.getSourceId());
        assertEquals("district-1", event.getDistrictId());
        assertNotNull(event.getTimestamp());
        assertEquals("Critical reading: 200.0 AQI", event.getMessage());
    }

    @Test
    void changesSubscriptionsInsideHandler() {
        Sensor air = Main.createSensors().get(0);
        List<SensorEvent> firstEvents = new ArrayList<>();
        List<SensorEvent> secondEvents = new ArrayList<>();
        Observer<SensorEvent> second = event -> secondEvents.add(event);
        Observer<SensorEvent> first = new Observer<SensorEvent>() {
            @Override
            public void update(SensorEvent event) {
                firstEvents.add(event);
                air.unsubscribe(event.getType(), this);
                air.subscribe(event.getType(), second);
            }
        };
        air.subscribe(EventType.AIR_POLLUTION_HIGH, first);

        air.measure(200.0);
        assertEquals(1, firstEvents.size());
        assertTrue(secondEvents.isEmpty());
        air.measure(201.0);
        assertEquals(1, firstEvents.size());
        assertEquals(1, secondEvents.size());

        air.unsubscribe(EventType.AIR_POLLUTION_HIGH, second);
        air.measure(202.0);
        assertEquals(1, secondEvents.size());
    }

    @Test
    void checksSensorThresholdsInvalidReadingsAndOfflineEvents() {
        List<Sensor> sensors = Main.createSensors();
        double[] thresholds = {150.0, 5.0, 60.0};
        String[] units = {"AQI", "m", "vehicles/km"};
        EventType[] types = {EventType.AIR_POLLUTION_HIGH, EventType.WATER_LEVEL_CRITICAL, EventType.TRAFFIC_JAM};

        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            LoggingService log = new LoggingService();
            for (EventType type : EventType.values()) {
                sensor.subscribe(type, log);
            }
            sensor.measure(0.0);
            sensor.measure(thresholds[i]);
            for (double invalid : new double[]{-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                assertThrows(IllegalArgumentException.class, () -> sensor.measure(invalid));
            }
            assertTrue(log.getEvents().isEmpty());

            sensor.measure(thresholds[i] + 1.0);
            sensor.markOffline();
            assertEquals(2, log.getEvents().size());
            assertEquals(types[i], log.getEvents().get(0).getType());
            assertTrue(log.getEvents().get(0).getMessage().endsWith(" " + units[i]));
            assertEquals(EventType.SENSOR_OFFLINE, log.getEvents().get(1).getType());
        }
    }

    @Test
    void aggregatesDistrictEventsWithoutCountingDerivedEventsTwice() {
        List<Sensor> sensors = Main.createSensors();
        StatisticsTracker statistics = new StatisticsTracker();
        LoggingService log = new LoggingService();
        DistrictAggregator aggregator = new DistrictAggregator("aggregator");
        for (Sensor sensor : sensors) {
            for (EventType type : EventType.values()) {
                if (type.isSensorEvent()) {
                    sensor.subscribe(type, statistics);
                    sensor.subscribe(type, aggregator);
                    sensor.subscribe(type, log);
                }
            }
        }
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, log);
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, statistics);
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, aggregator);

        sensors.get(0).measure(200.0);
        sensors.get(1).measure(6.0);
        List<SensorEvent> snapshot = log.getEvents();
        sensors.get(2).measure(80.0);
        SensorEvent inspection = new SensorEvent(EventType.INSPECTION_COMPLETED, "alerts", "district-1",
                LocalDateTime.of(2026, 10, 31, 12, 0), "Done");
        statistics.update(inspection);
        aggregator.update(inspection);

        assertEquals(3, statistics.getTotalCount());
        assertEquals(0, statistics.getCount(EventType.DISTRICT_SUMMARY));
        assertEquals(0, statistics.getCount(EventType.INSPECTION_COMPLETED));
        assertEquals(2, aggregator.getCount("district-1"));
        assertEquals(1, aggregator.getCount("district-2"));
        assertEquals(6, log.getEvents().size());
        assertEquals(4, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        int summaries = 0;
        for (SensorEvent event : log.getEvents()) {
            if (event.getType() == EventType.DISTRICT_SUMMARY) {
                assertEquals("aggregator", event.getSourceId());
                summaries++;
            }
        }
        assertEquals(3, summaries);
    }
}
