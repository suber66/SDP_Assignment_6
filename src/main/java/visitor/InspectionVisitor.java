package visitor;

import java.util.ArrayList;
import java.util.List;

public class InspectionVisitor implements CityVisitor<List<Device>> {
    private static final int MAX_OPERATING_HOURS = 10000;
    private static final int MAX_DAYS_WITHOUT_CALIBRATION = 365;

    @Override
    public List<Device> visit(City city) {
        List<Device> devices = new ArrayList<>();
        for (District district : city.getDistricts()) {
            devices.addAll(district.accept(this));
        }
        return List.copyOf(devices);
    }

    @Override
    public List<Device> visit(District district) {
        List<Device> devices = new ArrayList<>();
        for (Device device : district.getDevices()) {
            devices.addAll(device.accept(this));
        }
        return List.copyOf(devices);
    }

    @Override
    public List<Device> visit(StreetLight light) {
        if (light.getOperatingHours() >= MAX_OPERATING_HOURS) {
            return List.of(light);
        }
        return List.of();
    }

    @Override
    public List<Device> visit(TrafficSignal signal) {
        if (signal.getFaultySections() > 0) {
            return List.of(signal);
        }
        return List.of();
    }

    @Override
    public List<Device> visit(AirSensorNode sensor) {
        if (sensor.getDaysSinceCalibration() >= MAX_DAYS_WITHOUT_CALIBRATION) {
            return List.of(sensor);
        }
        return List.of();
    }

    @Override
    public List<Device> visit(WaterSensorNode sensor) {
        if (sensor.isWaterDamageDetected()) {
            return List.of(sensor);
        }
        return List.of();
    }
}
