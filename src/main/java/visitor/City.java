package visitor;

import java.util.List;

public final class City extends CityElement {
    private final List<District> districts;

    public City(String id, String name, List<District> districts) {
        super(id, name);
        if (districts == null) {
            throw new IllegalArgumentException("districts must not be null");
        }
        for (District district : districts) {
            if (district == null) {
                throw new IllegalArgumentException("District must not be null");
            }
        }
        this.districts = List.copyOf(districts);
    }

    public List<District> getDistricts() {
        return districts;
    }

    @Override
    public <R> R accept(CityVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
