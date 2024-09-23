package sebfisch.concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SingleThreadExecutor implements Executor {
    private final Thread worker;
    private final LinkedList<Runnable> taskQueue = new LinkedList<>();
    // TODO Task 1.1: add a boolean flag to indicate shutdown

    public SingleThreadExecutor() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public synchronized void execute(Runnable task) {
        // TODO Task 1.1: reject tasks after shutdown
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
            } // TODO Task 1.5: terminate when appropriate by handling `InterruptedException`
            catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
        // TODO Task 1.4 signal threads waiting for termination
    }

    private synchronized Runnable getQueuedTask() throws InterruptedException {
        while (taskQueue.isEmpty()) {
            // TODO Task 1.1: throw `InterruptedException` after shutdown
            wait();
        }
        return taskQueue.removeFirst();
    }

    public synchronized void shutdown() {
        // TODO Task 1.1: implement shutdown method
        throw new UnsupportedOperationException();
    }

    public synchronized boolean isShutdown() {
        // TODO Task 1.2: implement isShutdown method
        throw new UnsupportedOperationException();
    }

    public synchronized boolean isTerminated() {
        // TODO Task 1.3: implement isTerminated method
        throw new UnsupportedOperationException();
    }

    public synchronized void awaitTermination() throws InterruptedException {
        // TODO Task 1.4: implement awaitTermination method
        throw new UnsupportedOperationException();
    }

    public synchronized List<Runnable> shutdownNow() {
        // TODO Task 1.5: implement shutdownNow method
        throw new UnsupportedOperationException();
    }
}
