package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class NewThreadPerTaskExecutorTest {
    @Test
    public void testThatNewThreadIsCreatedForEachTask() throws InterruptedException {
        final int taskCount = 10;
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        final Set<String> threadNames = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    synchronized (threadNames) {
                        threadNames.add(Thread.currentThread().getName());
                    }
                }));
        executor.shutdown();
        executor.awaitTermination();
        assertEquals(taskCount, threadNames.size());
    }

    @Test
    public void testShutdown() {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testSubmittingTaskAfterShutdown() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        executor.shutdown();
        assertThrows(IllegalStateException.class, () -> executor.execute(() -> {
        }));
    }

    @Test
    public void testGracefulTerminationWithoutTasks() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        executor.shutdown();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testGracefulTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 1;
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(sleepSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        executor.shutdown();
        executor.awaitTermination(); // takes about 1 second
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testImmediateTerminationWithoutTasks() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testImmediateTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 10;
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
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

    @Test
    public void testSubmit() throws Exception {
        final int result = 42;
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        assertEquals(result, executor.submit(() -> 42).get());
    }

    @Test
    public void testFutureStatesWithNormalExecution() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
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
    public void testFutureStateWithCancelledExecution() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
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
    public void testFutureStateWithFailedExecution() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
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
    public void testFutureResultWithInterruptedExecution() throws InterruptedException {
        final NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        final Future<Void> future = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(1); // propagate InterruptedException
            return null;
        });
        assertEquals(Future.State.RUNNING, future.state());
        executor.shutdownNow(); // interrupts sleeping task
        executor.awaitTermination();
        assertThrows(ExecutionException.class, future::get);
    }
}
