package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class OwnSingleThreadExecutorServiceTest {
    @Test
    public void testThatSameThreadIsUsedForEachTask() throws InterruptedException {
        final int taskCount = 10;
        final int threadCount = 1;
        final Set<String> threadNames = new HashSet<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            IntStream.range(0, taskCount)
                    .forEach(n -> executor.execute(() -> {
                        synchronized (threadNames) {
                            threadNames.add(Thread.currentThread().getName());
                        }
                    }));
        } // auto close shuts down the executor and waits for termination
        assertEquals(threadCount, threadNames.size());
    }

    @Test
    public void testShutdown() {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            executor.shutdown();
            assertTrue(executor.isShutdown());
        }
    }

    @Test
    public void testSubmittingTaskAfterShutdown() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            executor.shutdown();
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {
            }));
        }
    }

    @Test
    public void testGracefulTerminationWithoutTasks() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void testGracefulTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepMillis = 100;
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            IntStream.range(0, taskCount)
                    .forEach(n -> executor.execute(() -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(sleepMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS); // takes about 10 * 100ms = 1s
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void testImmediateTerminationWithoutTasks() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void testImmediateTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 10;
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
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
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void testSubmit() throws InterruptedException, ExecutionException {
        final int result = 42;
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            assertEquals(result, executor.submit(() -> 42).get());
        }
    }

    @Test
    public void testFutureStatesWithNormalExecution() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
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
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertEquals(Future.State.SUCCESS, future.state());
        }
    }

    @Test
    public void testFutureStateWithCancelledExecution() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
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
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertThrows(CancellationException.class, future::get);
        }
    }

    @Test
    public void testFutureStateWithFailedExecution() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            final Future<Void> future = executor.submit(() -> {
                throw new RuntimeException("failure");
            });
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertEquals(Future.State.FAILED, future.state());
            assertThrows(ExecutionException.class, future::get);
        }
    }

    @Test
    public void testFutureResultWithInterruptedExecution() throws InterruptedException {
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            final Future<Void> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(10);
                return null;
            });
            assertEquals(Future.State.RUNNING, future.state());
            TimeUnit.SECONDS.sleep(1); // wait for task to start
            List<Runnable> pending = executor.shutdownNow();
            assertTrue(pending.isEmpty());
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertThrows(ExecutionException.class, future::get);
        }
    }

    @Test
    public void testSubmittingTasksConcurrently() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            IntStream.range(0, taskCount)
                    .parallel()
                    .forEach(n -> executor.execute(() -> {
                        synchronized (taskNumbers) {
                            taskNumbers.add(n);
                        }
                    }));
        }
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    public void testThatSubmittedTasksAreExecutedAfterShutdown() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            IntStream.range(0, taskCount)
                    .forEach(n -> executor.submit(() -> {
                        TimeUnit.MILLISECONDS.sleep(100);
                        synchronized (taskNumbers) {
                            taskNumbers.add(n);
                        }
                        return n;
                    }));
        }
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    public void testThatImmediateShutdownInterruptsExecution() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            IntStream.range(0, taskCount)
                    .forEach(n -> executor.submit(() -> {
                        TimeUnit.MILLISECONDS.sleep(100);
                        synchronized (taskNumbers) {
                            taskNumbers.add(n);
                        }
                        return n;
                    }));
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.HOURS);
            assertTrue(taskNumbers.isEmpty());
        }
    }

    @Test
    public void testThatCancellingOneTaskDoesNotInterfereWithOthers()
            throws InterruptedException, ExecutionException {
        int taskCount = 10;
        int cancelledTask = 5;
        Set<Integer> taskNumbers = new HashSet<>();
        List<Future<Integer>> futures = new ArrayList<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            futures.addAll(IntStream.range(0, taskCount)
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
                    .toList());
            futures.get(cancelledTask).cancel(true);
        }
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
        List<Future<Integer>> futures = new ArrayList<>();
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
            executor.execute(() -> {
                Thread.currentThread().interrupt();
            });
            TimeUnit.MILLISECONDS.sleep(300); // wait for task to be executed
            futures.addAll(IntStream.range(1, taskCount)
                    .mapToObj(n -> executor.submit(() -> {
                        synchronized (taskNumbers) {
                            taskNumbers.add(n);
                        }
                        return n;
                    }))
                    .toList());
        }
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
        try (final ExecutorService executor = new SingleThreadExecutorService()) {
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
}
