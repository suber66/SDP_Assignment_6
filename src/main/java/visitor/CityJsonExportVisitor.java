package visitor;

import java.util.List;

public class CityJsonExportVisitor implements CityVisitor<String> {
    @Override
    public String visit(City city) {
        return exportContainer("City", city, city.getDistricts());
    }

    @Override
    public String visit(District district) {
        return exportContainer("District", district, district.getDevices());
    }

    @Override
    public String visit(StreetLight light) {
        return exportDevice("StreetLight", light, "operatingHours", Integer.toString(light.getOperatingHours()));
    }

    @Override
    public String visit(TrafficSignal signal) {
        return exportDevice("TrafficSignal", signal, "faultySections", Integer.toString(signal.getFaultySections()));
    }

    @Override
    public String visit(AirSensorNode sensor) {
        String days = Integer.toString(sensor.getDaysSinceCalibration());
        return exportDevice("AirSensorNode", sensor, "daysSinceCalibration", days);
    }

    @Override
    public String visit(WaterSensorNode sensor) {
        String damaged = Boolean.toString(sensor.isWaterDamageDetected());
        return exportDevice("WaterSensorNode", sensor, "waterDamageDetected", damaged);
    }

    private String exportContainer(String type, CityElement container, List<? extends CityElement> children) {
        StringBuilder json = new StringBuilder("{\n");
        json.append(elementFields(type, container));
        json.append(",\n  \"children\": [");
        if (!children.isEmpty()) {
            json.append('\n');
            for (int i = 0; i < children.size(); i++) {
                CityElement child = children.get(i);
                String childJson = child.accept(this);
                json.append(indent(childJson));
                if (i < children.size() - 1) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("  ");
        }
        json.append("]\n}");
        return json.toString();
    }

    private String exportDevice(String type, Device device, String propertyName, String propertyValue) {
        return "{\n" + elementFields(type, device)
                + ",\n  \"powerWatts\": " + device.getPowerWatts()
                + ",\n  \"annualMaintenanceCost\": " + device.getAnnualMaintenanceCost()
                + ",\n  " + quote(propertyName) + ": " + propertyValue + "\n}";
    }

    private String elementFields(String type, CityElement element) {
        return "  \"type\": " + quote(type)
                + ",\n  \"id\": " + quote(element.getId())
                + ",\n  \"name\": " + quote(element.getName());
    }

    private String indent(String json) {
        return "    " + json.replace("\n", "\n    ");
    }

    private String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
                }
            }
        }
        quoted.append('"');
        return quoted.toString();
    }
}
