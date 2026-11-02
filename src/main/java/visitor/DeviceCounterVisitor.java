package visitor;

public class DeviceCounterVisitor implements CityVisitor<Integer>{

    @Override
    public Integer visit(City city) {
        int total = 0;
        for (District district : city.getDistricts()) {
            total += district.accept(this);
        }
        return total;
    }

    @Override
    public Integer visit(District district) {
        int total = 0;
        for (Device device : district.getDevices()) {
            total += device.accept(this);
        }
        return total;
    }

    @Override
    public Integer visit(StreetLight light) {
        return 1;
    }

    @Override
    public Integer visit(TrafficSignal signal) {
        return 1;
    }

    @Override
    public Integer visit(AirSensorNode sensor) {
        return 1;
    }

    @Override
    public Integer visit(WaterSensorNode sensor) {
        return 1;
    }
}
