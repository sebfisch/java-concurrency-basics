package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class SingleThreadExecutorTest {
    @Test
    public void testThatSameThreadIsUsedForEachTask() throws InterruptedException {
        final int taskCount = 10;
        final int threadCount = 1;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final Set<String> threadNames = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    synchronized (threadNames) {
                        threadNames.add(Thread.currentThread().getName());
                    }
                }));
        executor.shutdown();
        executor.awaitTermination();
        assertEquals(threadCount, threadNames.size());
    }

    @Test
    public void testShutdown() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testSubmittingTaskAfterShutdown() throws InterruptedException {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        executor.shutdown();
        assertThrows(IllegalStateException.class, () -> executor.execute(() -> {
        }));
    }

    @Test
    public void testGracefulTerminationWithoutTasks() throws InterruptedException {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        executor.shutdown();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testGracefulTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepMillis = 100;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        executor.shutdown();
        executor.awaitTermination(); // takes about 1 second (10 * 100ms)
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testImmediateTerminationWithoutTasks() throws InterruptedException {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testImmediateTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 10;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(sleepSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        executor.shutdownNow(); // causes InterruptedException in sleeping tasks
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }
}
