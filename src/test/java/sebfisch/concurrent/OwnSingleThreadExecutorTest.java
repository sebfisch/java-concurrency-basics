package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class OwnSingleThreadExecutorTest {
    private SingleThreadExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new SingleThreadExecutor();
    }

    @Test
    @Disabled
    public void testThatSameThreadIsUsedForEachTask() throws InterruptedException {
        final int taskCount = 10;
        final int threadCount = 1;

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
    @Disabled
    public void testShutdown() {
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    @Disabled
    public void testSubmittingTaskAfterShutdown() throws InterruptedException {
        executor.shutdown();
        assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {
        }));
    }

    @Test
    @Disabled
    public void testGracefulTerminationWithoutTasks() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    @Disabled
    public void testGracefulTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepMillis = 100;
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
    @Disabled
    public void testImmediateTerminationWithoutTasks() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    @Disabled
    public void testImmediateTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 10;
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(sleepSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        TimeUnit.SECONDS.sleep(1); // wait for first task to start
        List<Runnable> pending = executor.shutdownNow();
        assertEquals(taskCount - 1, pending.size());
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testSubmit() throws InterruptedException, ExecutionException {
        final int result = 42;
        assertEquals(result, executor.submit(() -> 42).get());
    }

    @Test
    @Disabled
    public void testFutureStatesWithNormalExecution() throws InterruptedException {
        final Future<Void> future = executor.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertEquals(Future.State.RUNNING, future.state());
        executor.shutdown();
        executor.awaitTermination();
        assertEquals(Future.State.SUCCESS, future.state());
    }

    @Test
    @Disabled
    public void testFutureStateWithCancelledExecution() throws InterruptedException {
        final Future<Void> future = executor.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        future.cancel(true);
        assertEquals(Future.State.CANCELLED, future.state());
        executor.shutdown();
        executor.awaitTermination();
        assertThrows(CancellationException.class, future::get);
    }

    @Test
    @Disabled
    public void testFutureStateWithFailedExecution() throws InterruptedException {
        final Future<Void> future = executor.submit(() -> {
            throw new RuntimeException("failure");
        });
        assertEquals(Future.State.RUNNING, future.state());
        executor.shutdown();
        executor.awaitTermination();
        assertEquals(Future.State.FAILED, future.state());
        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @Disabled
    public void testFutureResultWithInterruptedExecution() throws InterruptedException {
        final Future<Void> future = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(10);
            return null;
        });
        assertEquals(Future.State.RUNNING, future.state());
        TimeUnit.SECONDS.sleep(1); // wait for task to start
        List<Runnable> pending = executor.shutdownNow();
        assertTrue(pending.isEmpty());
        executor.awaitTermination();
        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @Disabled
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
        executor.awaitTermination();
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    @Disabled
    public void testThatSubmittedTasksAreExecutedAfterShutdown() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.submit(() -> {
                    TimeUnit.MILLISECONDS.sleep(100);
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }));
        executor.shutdown();
        executor.awaitTermination();
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    @Disabled
    public void testThatImmediateShutdownInterruptsExecution() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.submit(() -> {
                    TimeUnit.MILLISECONDS.sleep(100);
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                    return n;
                }));
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(taskNumbers.isEmpty());
    }

    @Test
    @Disabled
    public void testThatCancellingOneTaskDoesNotInterfereWithOthers()
            throws InterruptedException, ExecutionException {
        int taskCount = 10;
        int cancelledTask = 5;
        Set<Integer> taskNumbers = new HashSet<>();
        List<Future<Integer>> futures = IntStream.range(0, taskCount)
                .mapToObj(n -> executor.submit(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
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
        executor.awaitTermination();
        assertFalse(taskNumbers.contains(cancelledTask));
        assertEquals(taskCount - 1, taskNumbers.size());
        for (int i = 0; i < taskCount; i++) {
            if (i == cancelledTask) {
                assertTrue(futures.get(i).isCancelled());
            } else {
                assertEquals(i, futures.get(i).get());
            }
        }
    }

    @Test
    @Disabled
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
        executor.awaitTermination();
        assertFalse(taskNumbers.contains(0));
        assertEquals(taskCount - 1, taskNumbers.size());
        for (int i = 1; i < taskCount; i++) {
            assertEquals(i, futures.get(i - 1).get());
        }
    }
}
