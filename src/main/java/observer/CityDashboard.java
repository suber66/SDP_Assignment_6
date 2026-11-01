package observer;

import java.io.PrintStream;

public class CityDashboard implements Observer<SensorEvent> {
    private final PrintStream output;

    public CityDashboard(PrintStream output) {
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        this.output = output;
    }

    @Override
    public void update(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        output.println("[" + event.getTimestamp() + "] " + event.getType()
                + " | source=" + event.getSourceId()
                + " | district=" + event.getDistrictId()
                + " | " + event.getMessage());
    }
}
