package visitor;

public final class AirSensorNode extends Device {
    private final int daysSinceCalibration;

    public AirSensorNode(String id, String name, double powerWatts, double annualMaintenanceCost, int daysSinceCalibration) {
        super(id, name, powerWatts, annualMaintenanceCost);
        if (daysSinceCalibration < 0) {
            throw new IllegalArgumentException("daysSinceCalibration must be non-negative");
        }
        this.daysSinceCalibration = daysSinceCalibration;
    }

    public int getDaysSinceCalibration() {
        return daysSinceCalibration;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
