package sebfisch.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class NewThreadPerTaskExecutor implements Executor {
    private boolean isShutdown = false;
    private final List<Thread> activeWorkers = new ArrayList<>();

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        Thread worker = new Thread(() -> runTask(task));
        activeWorkers.add(worker);
        worker.start();
    }

    public <T> CompletableFuture<T> submit(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private void runTask(Runnable task) {
        try {
            task.run();
        } finally {
            synchronized (this) {
                activeWorkers.remove(Thread.currentThread());
                if (isTerminated()) {
                    notifyAll();
                }
            }
        }
    }

    public synchronized boolean isShutdown() {
        return isShutdown;
    }

    public synchronized void shutdown() {
        isShutdown = true;
        if (isTerminated()) {
            notifyAll();
        }
    }

    public synchronized void shutdownNow() {
        shutdown();
        for (Thread worker : activeWorkers) {
            worker.interrupt();
        }
    }

    public synchronized boolean isTerminated() {
        return isShutdown && activeWorkers.isEmpty();
    }

    public synchronized void awaitTermination() throws InterruptedException {
        while (!isTerminated()) {
            wait();
        }
    }
}
