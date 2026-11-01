package observer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DistrictAggregator extends AbstractSubject implements Observer<SensorEvent> {
    private final String id;
    private final Map<String, Integer> districtCounts = new HashMap<>();

    public DistrictAggregator(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        this.id = id;
    }

    @Override
    public void update(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        if (!event.getType().isSensorEvent()) {
            return;
        }

        int count;
        synchronized (districtCounts) {
            Integer previousCount = districtCounts.get(event.getDistrictId());
            if (previousCount == null) {
                count = 1;
            } else {
                count = previousCount + 1;
            }
            districtCounts.put(event.getDistrictId(), count);
        }

        SensorEvent summary = new SensorEvent(
                EventType.DISTRICT_SUMMARY,
                id,
                event.getDistrictId(),
                LocalDateTime.now(),
                "District sensor events: " + count
        );
        notifyListeners(summary);
    }

    public int getCount(String districtId) {
        if (districtId == null) {
            throw new IllegalArgumentException("districtId must not be null");
        }

        synchronized (districtCounts) {
            Integer count = districtCounts.get(districtId);
            if (count == null) {
                return 0;
            }
            return count;
        }
    }
}
