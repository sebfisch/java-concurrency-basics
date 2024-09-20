package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
        TimeUnit.SECONDS.sleep(1); // wait for first task to start
        List<Runnable> pending = executor.shutdownNow();
        assertEquals(taskCount - 1, pending.size());
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testSubmit() throws InterruptedException, ExecutionException {
        final int result = 42;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        assertEquals(result, executor.submit(() -> 42).get());
    }

    @Test
    public void testFutureStatesWithNormalExecution() throws InterruptedException {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
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
        final SingleThreadExecutor executor = new SingleThreadExecutor();
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
        final SingleThreadExecutor executor = new SingleThreadExecutor();
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
        final SingleThreadExecutor executor = new SingleThreadExecutor();
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
    public void testCompletableFutureAccept() throws InterruptedException {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        future.thenAccept(result -> assertEquals(42, result)).join();
    }

    @Test
    public void testCompletableFutureApply() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> mapped = future.thenApply(Object::toString);
        assertEquals("42", mapped.join());
    }

    @Test
    public void testCompletableFutureCompose() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> composed = future
                .thenCompose(result -> executor.submit(() -> result.toString()));
        assertEquals("42", composed.join());
    }

    @Test
    public void testCompletableFutureApplyAsync() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> mapped = future.thenApplyAsync(Object::toString, executor);
        assertEquals("42", mapped.join());
    }

    @Test
    public void testCompletableFutureExceptionally() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> {
            throw new RuntimeException("failure");
        });
        final CompletableFuture<Integer> recovered = future.exceptionally(exception -> 0);
        assertEquals(0, recovered.join());
    }

    @Test
    public void testCompletableFutureWhenCompleteNormally() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<Integer> completed = future.whenComplete((result, exception) -> {
            assertEquals(42, result);
            assertEquals(null, exception);
        });
        assertEquals(42, completed.join());
    }

    @Test
    public void testCompletableFutureWhenCompleteExceptionally() {
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        final CompletableFuture<Integer> future = executor.submit(() -> {
            throw new RuntimeException("failure");
        });
        final CompletableFuture<Integer> completed = future.whenComplete((result, exception) -> {
            assertEquals(null, result);
            assertEquals(RuntimeException.class, exception.getClass());
        });
        assertThrows(ExecutionException.class, completed::get);
    }

    @Test
    public void testCompletableFutureAllOf() {
        final int taskCount = 10;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        @SuppressWarnings("unchecked")
        final CompletableFuture<Integer>[] futures = IntStream.range(0, taskCount)
                .mapToObj(n -> executor.submit(() -> n))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
        for (CompletableFuture<Integer> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    public void testCompletableFutureAnyOf() {
        final int taskCount = 10;
        final SingleThreadExecutor executor = new SingleThreadExecutor();
        @SuppressWarnings("unchecked")
        final CompletableFuture<Integer>[] futures = IntStream.range(0, taskCount)
                .mapToObj(n -> executor.submit(() -> {
                    TimeUnit.SECONDS.sleep(n);
                    return n;
                }))
                .toArray(CompletableFuture[]::new);
        assertEquals(0, CompletableFuture.anyOf(futures).join());
    }
}
