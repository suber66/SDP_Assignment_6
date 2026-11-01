import org.junit.jupiter.api.Test;
import visitor.AirSensorNode;
import visitor.City;
import visitor.CityJsonExportVisitor;
import visitor.Device;
import visitor.District;
import visitor.EnergyConsumptionVisitor;
import visitor.InspectionVisitor;
import visitor.MaintenanceCostVisitor;
import visitor.StreetLight;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitorTest {
    @Test
    void calculatesPowerForCityAndSelectedDistricts() {
        City city = Main.createCity();
        EnergyConsumptionVisitor visitor = new EnergyConsumptionVisitor();

        assertEquals(570.0, city.accept(visitor), 0.000001);
        assertEquals(330.0, city.getDistricts().get(0).accept(visitor), 0.000001);
        assertEquals(240.0, city.getDistricts().get(1).accept(visitor), 0.000001);
        assertEquals(0.0, new City("empty", "Empty", List.of()).accept(visitor), 0.000001);
    }

    @Test
    void accumulatesMaintenanceCostAndDeviceCount() {
        City city = Main.createCity();
        MaintenanceCostVisitor visitor = new MaintenanceCostVisitor();

        double firstCost = city.getDistricts().get(0).accept(visitor);
        assertEquals(10000.0, firstCost, 0.000001);
        assertEquals(4, visitor.getDeviceCount());
        double totalCost = city.getDistricts().get(1).accept(visitor);
        assertEquals(18000.0, totalCost, 0.000001);
        assertEquals(8, visitor.getDeviceCount());
        assertEquals(10000.0, firstCost, 0.000001);

        MaintenanceCostVisitor wholeCityVisitor = new MaintenanceCostVisitor();
        assertEquals(18000.0, city.accept(wholeCityVisitor), 0.000001);
        assertEquals(8, wholeCityVisitor.getDeviceCount());
    }

    @Test
    void filtersDevicesAndChecksInspectionBoundaries() {
        City city = Main.createCity();
        InspectionVisitor visitor = new InspectionVisitor();

        assertEquals(List.of("light-1", "air-node-1", "signal-2", "water-node-2"), ids(city.accept(visitor)));
        assertEquals(List.of("light-1", "air-node-1"), ids(city.getDistricts().get(0).accept(visitor)));
        assertEquals(List.of("signal-2", "water-node-2"), ids(city.getDistricts().get(1).accept(visitor)));
        assertTrue(new City("empty", "Empty", List.of()).accept(visitor).isEmpty());

        StreetLight light = new StreetLight("light", "Light", 10.0, 100.0, 10000);
        AirSensorNode air = new AirSensorNode("air", "Air", 10.0, 100.0, 365);
        assertEquals(List.of(light), light.accept(visitor));
        assertEquals(List.of(air), air.accept(visitor));
    }

    @Test
    void exportsNestedJsonWithEscapingAndAllDeviceTypes() {
        StreetLight light = new StreetLight("light", "Light", 10.0, 100.0, 0);
        District district = new District("district", "District", List.of(light));
        City city = new City("city", "City", List.of(district));
        String expected = """
                {
                  "type": "City",
                  "id": "city",
                  "name": "City",
                  "children": [
                    {
                      "type": "District",
                      "id": "district",
                      "name": "District",
                      "children": [
                        {
                          "type": "StreetLight",
                          "id": "light",
                          "name": "Light",
                          "powerWatts": 10.0,
                          "annualMaintenanceCost": 100.0,
                          "operatingHours": 0
                        }
                      ]
                    }
                  ]
                }""";
        CityJsonExportVisitor visitor = new CityJsonExportVisitor();
        assertEquals(expected, city.accept(visitor));

        City escaped = new City("empty", "\"\\\n" + (char) 1, List.of());
        assertTrue(escaped.accept(visitor).contains("\"name\": \"\\\"\\\\\\n\\u0001\""));
        String fullJson = Main.createCity().accept(visitor);
        for (String type : List.of("StreetLight", "TrafficSignal", "AirSensorNode", "WaterSensorNode")) {
            assertTrue(fullJson.contains("\"type\": \"" + type + "\""));
        }
        assertTrue(fullJson.contains("\"faultySections\": 1"));
        assertTrue(fullJson.contains("\"daysSinceCalibration\": 400"));
        assertTrue(fullJson.contains("\"waterDamageDetected\": true"));
    }

    @Test
    void protectsCityAndDistrictListsFromExternalChanges() {
        Device light = new StreetLight("light", "Light", 10.0, 100.0, 0);
        List<Device> devices = new ArrayList<>(List.of(light));
        District district = new District("district", "District", devices);
        List<District> districts = new ArrayList<>(List.of(district));
        City city = new City("city", "City", districts);

        devices.clear();
        districts.clear();

        assertEquals(List.of(district), city.getDistricts());
        assertEquals(List.of(light), district.getDevices());
        assertThrows(UnsupportedOperationException.class, () -> city.getDistricts().clear());
        assertThrows(UnsupportedOperationException.class, () -> district.getDevices().clear());
    }

    private List<String> ids(List<Device> devices) {
        List<String> ids = new ArrayList<>();
        for (Device device : devices) {
            ids.add(device.getId());
        }
        return ids;
    }
}
