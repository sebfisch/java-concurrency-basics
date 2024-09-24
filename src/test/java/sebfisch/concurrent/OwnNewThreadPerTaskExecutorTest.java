package sebfisch.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OwnNewThreadPerTaskExecutorTest {
    private NewThreadPerTaskExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new NewThreadPerTaskExecutor();
    }

    @Test
    public void testThatNewThreadIsCreatedForEachTask() throws InterruptedException {
        final int taskCount = 10;
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
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testSubmittingTaskAfterShutdown() throws InterruptedException {
        executor.shutdown();
        assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {
        }));
    }

    @Test
    public void testGracefulTerminationWithoutTasks() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testAwaitTerminationBeforeShutdown() throws InterruptedException {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            executor.shutdown();
        }).start();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testGracefulTerminationWithSleepingTasks() throws InterruptedException {
        final int taskCount = 10;
        final int sleepSeconds = 1;
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
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
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
        executor.shutdownNow(); // causes InterruptedException in sleeping tasks
        executor.awaitTermination();
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testSubmit() throws InterruptedException, ExecutionException {
        final int result = 42;
        assertEquals(result, executor.submit(() -> 42).get());
    }

    @Test
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
    public void testFutureResultWithInterruptedExecution() throws InterruptedException {
        final Future<Void> future = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(1); // propagate InterruptedException
            return null;
        });
        assertEquals(Future.State.RUNNING, future.state());
        executor.shutdownNow(); // interrupts sleeping task
        executor.awaitTermination();
        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void testCompletableFutureAccept() throws InterruptedException {
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        future.thenAccept(result -> assertEquals(42, result)).join();
    }

    @Test
    public void testCompletableFutureApply() {
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> mapped = future.thenApply(Object::toString);
        assertEquals("42", mapped.join());
    }

    @Test
    public void testCompletableFutureCompose() {
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> composed = future
                .thenCompose(result -> executor.submit(() -> result.toString()));
        assertEquals("42", composed.join());
    }

    @Test
    public void testCompletableFutureApplyAsync() {
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<String> mapped = future.thenApplyAsync(Object::toString, executor);
        assertEquals("42", mapped.join());
    }

    @Test
    public void testCompletableFutureHandle() {
        final CompletableFuture<Integer> future = executor.submit(() -> {
            throw new RuntimeException("failure");
        });
        final CompletableFuture<Integer> recovered = future
                .handle((result, error) -> error == null ? result : 0);
        assertEquals(0, recovered.join());
    }

    @Test
    public void testCompletableFutureWhenCompleteNormally() {
        final CompletableFuture<Integer> future = executor.submit(() -> 42);
        final CompletableFuture<Integer> completed = future.whenComplete((result, exception) -> {
            assertEquals(42, result);
            assertEquals(null, exception);
        });
        assertEquals(42, completed.join());
    }

    @Test
    public void testCompletableFutureWhenCompleteExceptionally() {
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
        @SuppressWarnings("unchecked")
        final CompletableFuture<Integer>[] futures = IntStream.range(0, taskCount)
                .mapToObj(n -> executor.submit(() -> {
                    TimeUnit.SECONDS.sleep(n);
                    return n;
                }))
                .toArray(CompletableFuture[]::new);
        assertEquals(0, CompletableFuture.anyOf(futures).join());
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
        executor.awaitTermination();
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
        executor.awaitTermination();
        assertEquals(taskCount, taskNumbers.size());
    }

    @Test
    public void testThatImmediateShutdownInterruptsExecution() throws InterruptedException {
        final int taskCount = 10;
        final Set<Integer> taskNumbers = new HashSet<>();
        IntStream.range(0, taskCount)
                .forEach(n -> executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    synchronized (taskNumbers) {
                        taskNumbers.add(n);
                    }
                }));
        executor.shutdownNow();
        executor.awaitTermination();
        assertTrue(taskNumbers.isEmpty());
    }

    @Test
    public void testThatCancellingOneTaskDoesNotInterfereWithOthers()
            throws InterruptedException, ExecutionException {
        int taskCount = 10;
        int cancelledTask = 5;
        Set<Integer> taskNumbers = new HashSet<>();
        List<CompletableFuture<Integer>> futures = IntStream.range(0, taskCount)
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
        // cancel(true) does not interrupt with CompletableFuture
        futures.get(cancelledTask).cancel(true);
        executor.shutdown();
        executor.awaitTermination();
        assertTrue(taskNumbers.contains(cancelledTask));
        assertEquals(taskCount, taskNumbers.size());
        for (int i = 0; i < taskCount; i++) {
            if (i == cancelledTask) {
                assertTrue(futures.get(i).isCancelled());
            } else {
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
        List<CompletableFuture<Integer>> futures = IntStream.range(1, taskCount)
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
