package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class SingleThreadExecutorServiceTest {
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
            assertThrows(IllegalStateException.class, () -> executor.execute(() -> {
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
            assertEquals(Future.State.RUNNING, future.state());
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
}
