package visitor;

public abstract class CityElement {
    private final String id;
    private final String name;

    protected CityElement(String id, String name) {
        if (id == null || name == null) {
            throw new IllegalArgumentException("id and name must not be null");
        }
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract <R> R accept(CityVisitor<R> visitor);
}
