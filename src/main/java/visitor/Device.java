package visitor;

public abstract class Device extends CityElement {
    private final double powerWatts;
    private final double annualMaintenanceCost;

    protected Device(String id, String name, double powerWatts, double annualMaintenanceCost) {
        super(id, name);
        if (!Double.isFinite(powerWatts) || powerWatts < 0.0) {
            throw new IllegalArgumentException("powerWatts must be finite and non-negative");
        }
        if (!Double.isFinite(annualMaintenanceCost) || annualMaintenanceCost < 0.0) {
            throw new IllegalArgumentException("annualMaintenanceCost must be finite and non-negative");
        }
        this.powerWatts = powerWatts;
        this.annualMaintenanceCost = annualMaintenanceCost;
    }

    public double getPowerWatts() {
        return powerWatts;
    }

    public double getAnnualMaintenanceCost() {
        return annualMaintenanceCost;
    }
}
