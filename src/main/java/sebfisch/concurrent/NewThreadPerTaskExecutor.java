package sebfisch.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class NewThreadPerTaskExecutor implements Executor {
    private boolean isShutdown = false;
    private final List<Thread> activeThreads = new ArrayList<>();

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Executor has been shut down");
        }
        Thread worker = new Thread(() -> runTask(task));
        activeThreads.add(worker);
        worker.start();
    }

    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    private void runTask(Runnable task) {
        try {
            task.run();
        } finally {
            synchronized (this) {
                activeThreads.remove(Thread.currentThread());
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
    }

    public synchronized void shutdownNow() {
        shutdown();
        for (Thread worker : activeThreads) {
            worker.interrupt();
        }
    }

    public synchronized boolean isTerminated() {
        return isShutdown && activeThreads.isEmpty();
    }

    public synchronized void awaitTermination() throws InterruptedException {
        while (!isTerminated()) {
            wait();
        }
    }
}
