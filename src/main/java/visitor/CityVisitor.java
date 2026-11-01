package visitor;

public interface CityVisitor<R> {
    R visit(City city);
    R visit(District district);
    R visit(StreetLight light);
    R visit(TrafficSignal signal);
    R visit(AirSensorNode sensor);
    R visit(WaterSensorNode sensor);
}
