import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MonitoringSimulation {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ExecutorService executor;
    private final List<Runnable> producers;
    private final int iterations;
    private final int timeoutSeconds;

    public MonitoringSimulation(ExecutorService executor, List<Runnable> producers, int iterations) {
        this(executor, producers, iterations, DEFAULT_TIMEOUT_SECONDS);
    }

    public MonitoringSimulation(ExecutorService executor, List<Runnable> producers, int iterations, int timeoutSeconds) {
        if (executor == null || producers == null) {
            throw new IllegalArgumentException("executor and producers must not be null");
        }
        if (producers.size() < 3) {
            throw new IllegalArgumentException("At least 3 producers are required");
        }
        if (iterations <= 0 || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("iterations and timeoutSeconds must be positive");
        }
        for (Runnable producer : producers) {
            if (producer == null) {
                throw new IllegalArgumentException("Producer must not be null");
            }
        }
        this.executor = executor;
        this.producers = List.copyOf(producers);
        this.iterations = iterations;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void run() throws InterruptedException {
        List<Future<?>> results = new ArrayList<>();
        boolean completed = false;

        try {
            if (executor.isShutdown()) {
                throw new IllegalStateException("Simulation needs an executor that is not shut down");
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Simulation interrupted before starting");
            }

            for (Runnable producer : producers) {
                Future<?> result = executor.submit(() -> produce(producer));
                results.add(result);
            }

            executor.shutdown();
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Simulation exceeded its time limit");
            }

            for (Future<?> result : results) {
                result.get();
            }
            completed = true;
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A simulation producer failed", exception.getCause());
        } finally {
            if (!completed) {
                for (Future<?> result : results) {
                    result.cancel(true);
                }
                executor.shutdownNow();
            }

            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Simulation threads did not stop");
            }
        }
    }

    private void produce(Runnable producer) {
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Producer interrupted before completing all iterations");
            }
            producer.run();
        }
    }
}
