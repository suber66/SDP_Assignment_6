package observer;

import visitor.Device;
import visitor.District;
import visitor.InspectionVisitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertService extends AbstractSubject implements Observer<SensorEvent> {
    private final String id;
    private final Map<String, District> districts;

    public AlertService(String id, Map<String, District> districts) {
        if (id == null || districts == null) {
            throw new IllegalArgumentException("id and districts must not be null");
        }
        for (Map.Entry<String, District> entry : districts.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("District registry must not contain null");
            }
            if (!entry.getKey().equals(entry.getValue().getId())) {
                throw new IllegalArgumentException("District key must match its id");
            }
        }
        this.id = id;
        this.districts = Map.copyOf(districts);
    }

    @Override
    public void update(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!event.getType().isSensorEvent()) {
            return;
        }

        District district = districts.get(event.getDistrictId());
        if (district == null) {
            System.err.println("Unknown district: " + event.getDistrictId());
            return;
        }

        InspectionVisitor visitor = new InspectionVisitor();
        List<Device> devices = district.accept(visitor);
        SensorEvent result = new SensorEvent(
                EventType.INSPECTION_COMPLETED,
                id,
                district.getId(),
                LocalDateTime.now(),
                createMessage(event.getType(), devices)
        );
        notifyListeners(result);
    }

    private String createMessage(EventType cause, List<Device> devices) {
        List<String> deviceIds = new ArrayList<>();
        for (Device device : devices) {
            deviceIds.add(device.getId());
        }
        return "Inspection completed after " + cause + ": "
                + devices.size() + " device(s) require inspection " + deviceIds;
    }
}
