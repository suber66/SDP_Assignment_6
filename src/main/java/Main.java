import observer.AlertService;
import observer.CityDashboard;
import observer.DistrictAggregator;
import observer.EventType;
import observer.LoggingService;
import observer.Observer;
import observer.Sensor;
import observer.SensorEvent;
import observer.StatisticsTracker;
import visitor.AirSensorNode;
import visitor.City;
import visitor.CityJsonExportVisitor;
import visitor.District;
import visitor.EnergyConsumptionVisitor;
import visitor.MaintenanceCostVisitor;
import visitor.StreetLight;
import visitor.TrafficSignal;
import visitor.WaterSensorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_THREAD_COUNT = 3;
    private static final int DEFAULT_ITERATIONS = 5;
    private static final double AIR_LIMIT = 150.0;
    private static final double WATER_LIMIT_METERS = 5.0;
    private static final double TRAFFIC_LIMIT_VEHICLES_PER_KM = 60.0;

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 2) {
            throw new IllegalArgumentException("Expected at most two arguments");
        }
        int threadCount = DEFAULT_THREAD_COUNT;
        int iterations = DEFAULT_ITERATIONS;
        if (args.length >= 1) {
            threadCount = readNumber(args[0], "threads");
        }
        if (args.length == 2) {
            iterations = readNumber(args[1], "iterations");
        }
        if (threadCount < 3 || iterations <= 0) {
            throw new IllegalArgumentException("Use at least 3 threads and a positive iteration count");
        }

        City city = createCity();
        District firstDistrict = city.getDistricts().get(0);
        District secondDistrict = city.getDistricts().get(1);
        CityDashboard dashboard = new CityDashboard(System.out);
        StatisticsTracker statistics = new StatisticsTracker();
        LoggingService log = new LoggingService();
        DistrictAggregator aggregator = new DistrictAggregator("district-aggregator");
        AlertService alerts = new AlertService("alert-service", Map.of(
                firstDistrict.getId(), firstDistrict, secondDistrict.getId(), secondDistrict
        ));
        List<Sensor> sensors = createSensors();
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

        List<Runnable> producers = createProducers(threadCount, sensors);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        MonitoringSimulation simulation = new MonitoringSimulation(executor, producers, iterations);
        System.out.println("Simulation: " + threadCount + " threads, " + iterations + " readings per producer");
        simulation.run();

        printReports(city, statistics, log);
    }

    static List<Sensor> createSensors() {
        return List.of(
                new Sensor("air-1", "district-1", EventType.AIR_POLLUTION_HIGH, AIR_LIMIT, "AQI"),
                new Sensor("water-1", "district-1", EventType.WATER_LEVEL_CRITICAL, WATER_LIMIT_METERS, "m"),
                new Sensor("traffic-1", "district-2", EventType.TRAFFIC_JAM, TRAFFIC_LIMIT_VEHICLES_PER_KM, "vehicles/km")
        );
    }

    static City createCity() {
        District first = new District("district-1", "Central district", List.of(
                new StreetLight("light-1", "Main street light", 100.0, 1000.0, 12000),
                new TrafficSignal("signal-1", "Main traffic signal", 200.0, 2000.0, 0),
                new AirSensorNode("air-node-1", "Air station", 10.0, 3000.0, 400),
                new WaterSensorNode("water-node-1", "Water station", 20.0, 4000.0, false)
        ));
        District second = new District("district-2", "Riverside district", List.of(
                new StreetLight("light-2", "Riverside light", 50.0, 500.0, 500),
                new TrafficSignal("signal-2", "Riverside signal", 150.0, 1500.0, 1),
                new AirSensorNode("air-node-2", "Riverside air station", 15.0, 2500.0, 30),
                new WaterSensorNode("water-node-2", "Riverside water station", 25.0, 3500.0, true)
        ));
        return new City("city-1", "Smart city", List.of(first, second));
    }

    private static int readNumber(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static List<Runnable> createProducers(int count, List<Sensor> sensors) {
        List<Runnable> producers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            switch (i % 3) {
                case 0 -> producers.add(() -> sensors.get(0).measure(200.0));
                case 1 -> producers.add(() -> sensors.get(1).measure(6.0));
                case 2 -> producers.add(() -> sensors.get(2).measure(80.0));
            }
        }
        return producers;
    }

    private static void printReports(City city, StatisticsTracker statistics, LoggingService log) {
        MaintenanceCostVisitor maintenance = new MaintenanceCostVisitor();
        double annualCost = city.accept(maintenance);
        System.out.println("Total power: " + city.accept(new EnergyConsumptionVisitor()) + " W");
        System.out.println("Annual maintenance: " + annualCost + " KZT/year");
        System.out.println("Devices: " + maintenance.getDeviceCount());
        System.out.println("Sensor events: " + statistics.getTotalCount());
        System.out.println("Logged events: " + log.getEvents().size());
        System.out.println(city.accept(new CityJsonExportVisitor()));
    }
}
