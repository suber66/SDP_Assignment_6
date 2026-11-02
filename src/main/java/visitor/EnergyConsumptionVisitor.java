package visitor;

public class EnergyConsumptionVisitor implements CityVisitor<Double> {
    @Override
    public Double visit(City city) {
        double total = 0.0;
        for (District district : city.getDistricts()) {
            total += district.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(District district) {
        double total = 0.0;
        for (Device device : district.getDevices()) {
            total += device.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(StreetLight light) {
        return light.getPowerWatts();
    }

    @Override
    public Double visit(TrafficSignal signal) {
        return signal.getPowerWatts();
    }

    @Override
    public Double visit(AirSensorNode sensor) {
        return sensor.getPowerWatts();
    }

    @Override
    public Double visit(WaterSensorNode sensor) {
        return sensor.getPowerWatts();
    }

    @Override
    public Double visit(NewCityElement newCityElement) {
        return newCityElement.getPowerWatts();
    }
}
