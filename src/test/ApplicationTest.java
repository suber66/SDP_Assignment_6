import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {
    @Test
    void acceptsSettingsAndRejectsInvalidArguments() throws Exception {
        String defaultOutput = runMain();
        assertTrue(defaultOutput.contains("Sensor events: 15" + System.lineSeparator()));
        assertTrue(defaultOutput.contains("Logged events: 45" + System.lineSeparator()));

        String configuredOutput = runMain("4", "2");
        assertTrue(configuredOutput.contains("Sensor events: 8" + System.lineSeparator()));
        assertTrue(configuredOutput.contains("Logged events: 24" + System.lineSeparator()));
        assertThrows(IllegalArgumentException.class, () -> runMain("2", "5"));
        assertThrows(IllegalArgumentException.class, () -> runMain("3", "0"));
        assertThrows(IllegalArgumentException.class, () -> runMain("text"));
        assertThrows(IllegalArgumentException.class, () -> runMain("3", "5", "extra"));
    }

    private String runMain(String... arguments) throws InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream testOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(testOutput);
            Main.main(arguments);
            return output.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originalOutput);
        }
    }
}
