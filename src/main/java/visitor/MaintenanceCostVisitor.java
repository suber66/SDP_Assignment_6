package visitor;

public class MaintenanceCostVisitor implements CityVisitor<Double> {
    private double annualCost = 0.0;
    private int deviceCount = 0;

    @Override
    public Double visit(City city) {
        for (District district : city.getDistricts()) {
            district.accept(this);
        }
        return annualCost;
    }

    @Override
    public Double visit(District district) {
        for (Device device : district.getDevices()) {
            device.accept(this);
        }
        return annualCost;
    }

    @Override
    public Double visit(StreetLight light) {
        return addDevice(light);
    }

    @Override
    public Double visit(TrafficSignal signal) {
        return addDevice(signal);
    }

    @Override
    public Double visit(AirSensorNode sensor) {
        return addDevice(sensor);
    }

    @Override
    public Double visit(WaterSensorNode sensor) {
        return addDevice(sensor);
    }

    @Override
    public Double visit(NewCityElement newCityElement) {
        return addDevice(newCityElement);
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    private double addDevice(Device device) {
        annualCost += device.getAnnualMaintenanceCost();
        deviceCount++;
        return annualCost;
    }
}
