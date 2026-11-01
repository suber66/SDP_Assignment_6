package visitor;

import java.util.List;

public final class District extends CityElement {
    private final List<Device> devices;

    public District(String id, String name, List<Device> devices) {
        super(id, name);
        if (devices == null) {
            throw new IllegalArgumentException("devices must not be null");
        }
        for (Device device : devices) {
            if (device == null) {
                throw new IllegalArgumentException("Device must not be null");
            }
        }
        this.devices = List.copyOf(devices);
    }

    public List<Device> getDevices() {
        return devices;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
