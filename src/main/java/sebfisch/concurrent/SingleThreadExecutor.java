package sebfisch.concurrent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SingleThreadExecutor implements Executor {
    private final Thread worker;
    private final LinkedList<Runnable> queuedTasks = new LinkedList<>();
    private boolean isShutdown = false;

    public SingleThreadExecutor() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Executor has been shut down");
        }
        queuedTasks.addLast(task);
        notify();
    }

    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    private void runQueuedTasks() {
        while (true) {
            Runnable task;
            try {
                task = getQueuedTask();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                task.run();
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            } finally {
                synchronized (this) {
                    if (isTerminated()) {
                        notifyAll();
                    }
                }
            }
        }
    }

    private synchronized Runnable getQueuedTask() throws InterruptedException {
        while (queuedTasks.isEmpty()) {
            wait();
        }
        return queuedTasks.removeFirst();
    }

    public synchronized boolean isShutdown() {
        return isShutdown;
    }

    public synchronized void shutdown() {
        isShutdown = true;
        notifyAll();
    }

    public synchronized List<Runnable> shutdownNow() {
        isShutdown = true;
        final List<Runnable> pending = new ArrayList<>(queuedTasks);
        queuedTasks.clear();
        worker.interrupt();
        return pending;
    }

    public synchronized boolean isTerminated() {
        return isShutdown && queuedTasks.isEmpty();
    }

    public synchronized void awaitTermination() throws InterruptedException {
        while (!isTerminated()) {
            wait();
        }
    }
}
