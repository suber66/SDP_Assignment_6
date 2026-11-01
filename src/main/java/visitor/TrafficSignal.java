package visitor;

public final class TrafficSignal extends Device {
    private final int faultySections;

    public TrafficSignal(String id, String name, double powerWatts, double annualMaintenanceCost, int faultySections) {
        super(id, name, powerWatts, annualMaintenanceCost);
        if (faultySections < 0) {
            throw new IllegalArgumentException("faultySections must be non-negative");
        }
        this.faultySections = faultySections;
    }

    public int getFaultySections() {
        return faultySections;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
