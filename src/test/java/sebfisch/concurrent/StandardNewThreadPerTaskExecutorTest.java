package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StandardNewThreadPerTaskExecutorTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newThreadPerTaskExecutor(Thread::new);
    }

    @Test
    public void testSubmittingTasksConcurrently() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .parallel()
                .forEach(n -> executor.execute(() -> {
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                }));
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    public void testThatSubmittedTasksAreExecutedAfterShutdown() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.submit(() -> {
                    TimeUnit.SECONDS.sleep(1);
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }));
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    public void testThatImmediateShutdownInterruptsExecution() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.submit(() -> {
                    TimeUnit.SECONDS.sleep(1);
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }));
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(taskNumbers.isEmpty());
    }

    @Test
    public void testThatCancellingOneTaskDoesNotInterfereWithOthers()
            throws InterruptedException, ExecutionException {
        int taskCount = 10;
        int cancelledTask = 5;
        Set<Integer> taskNumbers = new HashSet<>();
        List<Future<Integer>> futures = IntStream.range(0, taskCount)
                .mapToObj(n -> executor.submit(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return n;
                    }
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }))
                .toList();
        futures.get(cancelledTask).cancel(true);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertFalse(taskNumbers.contains(cancelledTask));
        assertEquals(taskCount - 1, taskNumbers.size());
        for (int i = 0; i < taskCount; i++) {
            if (i != cancelledTask) {
                assertEquals(i, futures.get(i).get());
            }
        }
    }

    @Test
    public void testThatInterruptingOneTaskDoesNotInterfereWithOthers()
            throws InterruptedException, ExecutionException {
        int taskCount = 10;
        Set<Integer> taskNumbers = new HashSet<>();
        executor.execute(() -> {
            Thread.currentThread().interrupt();
        });
        TimeUnit.MILLISECONDS.sleep(300); // wait for task to be executed
        List<Future<Integer>> futures = IntStream.range(1, taskCount)
                .mapToObj(n -> executor.submit(() -> {
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }))
                .toList();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertFalse(taskNumbers.contains(0));
        assertEquals(taskCount - 1, taskNumbers.size());
        for (int i = 1; i < taskCount; i++) {
            assertEquals(i, futures.get(i - 1).get());
        }
    }

    @Test
    public void testImmediateShutdownAfterGracefulShutdown() throws InterruptedException {
        int taskCount = 10;
        Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.submit(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(n);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return n;
                    }
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }));
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(0 < taskNumbers.size() && taskNumbers.size() < taskCount);
    }
}
