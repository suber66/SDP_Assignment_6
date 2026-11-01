package visitor;

public final class WaterSensorNode extends Device {
    private final boolean waterDamageDetected;

    public WaterSensorNode(String id, String name, double powerWatts, double annualMaintenanceCost, boolean waterDamageDetected) {
        super(id, name, powerWatts, annualMaintenanceCost);
        this.waterDamageDetected = waterDamageDetected;
    }

    public boolean isWaterDamageDetected() {
        return waterDamageDetected;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
