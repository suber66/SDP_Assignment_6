package visitor;

public final class StreetLight extends Device {
    private final int operatingHours;

    public StreetLight(String id, String name, double powerWatts, double annualMaintenanceCost, int operatingHours) {
        super(id, name, powerWatts, annualMaintenanceCost);
        if (operatingHours < 0) {
            throw new IllegalArgumentException("operatingHours must be non-negative");
        }
        this.operatingHours = operatingHours;
    }

    public int getOperatingHours() {
        return operatingHours;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
