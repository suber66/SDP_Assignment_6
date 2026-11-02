package visitor;

public class NewCityElement extends Device{

    public NewCityElement(String id, String name, double powerWatts, double annualMaintenanceCost) {
        super(id, name, powerWatts, annualMaintenanceCost);
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
