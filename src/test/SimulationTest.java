import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import observer.AlertService;
import observer.CityDashboard;
import observer.DistrictAggregator;
import observer.EventType;
import observer.LoggingService;
import observer.Observer;
import observer.Sensor;
import observer.SensorEvent;
import observer.StatisticsTracker;
import visitor.City;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {
    private ExecutorService executor;

    @AfterEach
    void stopRemainingThreads() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void processesFourThousandEventsThroughAllFiveListeners() throws InterruptedException {
        City city = Main.createCity();
        List<Sensor> sensors = Main.createSensors();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CityDashboard dashboard = new CityDashboard(new PrintStream(output, true, StandardCharsets.UTF_8));
        StatisticsTracker statistics = new StatisticsTracker();
        LoggingService log = new LoggingService();
        DistrictAggregator aggregator = new DistrictAggregator("aggregator");
        AlertService alerts = new AlertService("alerts", Map.of(
                "district-1", city.getDistricts().get(0), "district-2", city.getDistricts().get(1)
        ));
        List<Observer<SensorEvent>> listeners = List.of(dashboard, statistics, log, aggregator, alerts);
        for (Sensor sensor : sensors) {
            for (EventType type : EventType.values()) {
                if (type.isSensorEvent()) {
                    for (Observer<SensorEvent> listener : listeners) {
                        sensor.subscribe(type, listener);
                    }
                }
            }
        }
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, dashboard);
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, log);
        alerts.subscribe(EventType.INSPECTION_COMPLETED, dashboard);
        alerts.subscribe(EventType.INSPECTION_COMPLETED, log);
        List<Runnable> producers = List.of(
                () -> sensors.get(0).measure(200.0),
                () -> sensors.get(1).measure(6.0),
                () -> sensors.get(2).measure(80.0),
                () -> sensors.get(0).measure(200.0)
        );
        executor = Executors.newFixedThreadPool(4);

        new MonitoringSimulation(executor, producers, 1000).run();

        assertTrue(executor.isTerminated());
        assertEquals(4000, statistics.getTotalCount());
        assertEquals(2000, statistics.getCount(EventType.AIR_POLLUTION_HIGH));
        assertEquals(1000, statistics.getCount(EventType.WATER_LEVEL_CRITICAL));
        assertEquals(1000, statistics.getCount(EventType.TRAFFIC_JAM));
        assertEquals(3000, aggregator.getCount("district-1"));
        assertEquals(1000, aggregator.getCount("district-2"));
        int summaries = 0;
        int inspections = 0;
        for (SensorEvent event : log.getEvents()) {
            if (event.getType() == EventType.DISTRICT_SUMMARY) {
                summaries++;
            } else if (event.getType() == EventType.INSPECTION_COMPLETED) {
                inspections++;
                if (event.getDistrictId().equals("district-1")) {
                    assertTrue(event.getMessage().endsWith("2 device(s) require inspection [light-1, air-node-1]"));
                } else {
                    assertEquals("district-2", event.getDistrictId());
                    assertTrue(event.getMessage().endsWith("2 device(s) require inspection [signal-2, water-node-2]"));
                }
            }
        }
        assertEquals(4000, summaries);
        assertEquals(4000, inspections);
        assertEquals(12000, log.getEvents().size());
        assertEquals(12000L, output.toString(StandardCharsets.UTF_8).lines().count());
    }

    @Test
    void allowsSubscriptionChangesWhileSummaryHandlerWaits() throws Exception {
        Sensor air = Main.createSensors().get(0);
        DistrictAggregator aggregator = new DistrictAggregator("aggregator");
        LoggingService rawLog = new LoggingService();
        LoggingService summaryLog = new LoggingService();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Observer<SensorEvent> slowListener = event -> {
            started.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        };
        air.subscribe(EventType.AIR_POLLUTION_HIGH, aggregator);
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, slowListener);
        aggregator.subscribe(EventType.DISTRICT_SUMMARY, summaryLog);
        executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> air.measure(200.0));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> {
                assertEquals(1, aggregator.getCount("district-1"));
                aggregator.unsubscribe(EventType.DISTRICT_SUMMARY, slowListener);
                air.subscribe(EventType.AIR_POLLUTION_HIGH, rawLog);
                air.measure(201.0);
            });
            second.get(3, TimeUnit.SECONDS);

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertEquals(2, aggregator.getCount("district-1"));
            assertEquals(2, summaryLog.getEvents().size());
            assertEquals(1, rawLog.getEvents().size());
        } finally {
            release.countDown();
        }
    }

    @Test
    void reportsProducerFailureAndStopsPool() {
        executor = Executors.newFixedThreadPool(3);
        IllegalArgumentException cause = new IllegalArgumentException("Broken producer");
        Runnable failing = () -> {
            throw cause;
        };
        Runnable normal = () -> { };
        MonitoringSimulation simulation = new MonitoringSimulation(executor, List.of(failing, normal, normal), 1);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> simulation.run());

        assertSame(cause, exception.getCause());
        assertTrue(executor.isTerminated());
    }

    @Test
    void cancelsBlockedProducersAfterTimeout() {
        executor = Executors.newFixedThreadPool(3);
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocking = () -> waitForRelease(release);
        MonitoringSimulation simulation = new MonitoringSimulation(executor, List.of(blocking, blocking, blocking), 1, 1);

        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> simulation.run());
            assertEquals("Simulation exceeded its time limit", exception.getMessage());
            assertTrue(executor.isTerminated());
        } finally {
            release.countDown();
        }
    }

    @Test
    void stopsWorkersWhenCallingThreadIsInterrupted() throws InterruptedException {
        executor = Executors.newFixedThreadPool(3);
        CountDownLatch started = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocking = () -> {
            started.countDown();
            waitForRelease(release);
        };
        MonitoringSimulation simulation = new MonitoringSimulation(executor, List.of(blocking, blocking, blocking), 1);
        List<Exception> failures = new ArrayList<>();
        Thread caller = new Thread(() -> {
            try {
                simulation.run();
            } catch (Exception exception) {
                failures.add(exception);
            }
        });

        try {
            caller.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(5000);

            assertFalse(caller.isAlive());
            assertEquals(1, failures.size());
            assertInstanceOf(InterruptedException.class, failures.get(0));
            assertTrue(executor.isTerminated());
        } finally {
            release.countDown();
            caller.interrupt();
            caller.join(5000);
        }
    }

    private void waitForRelease(CountDownLatch release) {
        try {
            release.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
