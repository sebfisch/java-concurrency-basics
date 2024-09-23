package sebfisch.concurrent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

public class SingleThreadExecutor implements Executor {
    private final Thread worker;
    private final LinkedList<Runnable> taskQueue = new LinkedList<>();
    private boolean isShutdown = false;

    public SingleThreadExecutor() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        taskQueue.addLast(task);
        notifyAll();
    }

    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    private void runQueuedTasks() {
        while (true) {
            try {
                getQueuedTask().run();
            } catch (InterruptedException e) {
                synchronized (this) {
                    if (isShutdown) {
                        break;
                    }
                }
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
        synchronized (this) {
            notifyAll();
        }
    }

    private synchronized Runnable getQueuedTask() throws InterruptedException {
        while (taskQueue.isEmpty()) {
            if (isShutdown) {
                throw new InterruptedException();
            }
            wait();
        }
        return taskQueue.removeFirst();
    }

    public synchronized void shutdown() {
        isShutdown = true;
    }

    public synchronized boolean isShutdown() {
        return isShutdown;
    }

    public synchronized boolean isTerminated() {
        return isShutdown && taskQueue.isEmpty();
    }

    public synchronized void awaitTermination() throws InterruptedException {
        while (!isTerminated()) {
            wait();
        }
    }

    public synchronized List<Runnable> shutdownNow() {
        isShutdown = true;
        final List<Runnable> remainingTasks = new ArrayList<>(taskQueue);
        taskQueue.clear();
        worker.interrupt();
        return remainingTasks;
    }
}
