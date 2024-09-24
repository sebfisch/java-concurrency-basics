package sebfisch.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class AlternativeSingleThreadExecutorService extends AbstractExecutorService {
    private Thread worker;
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private volatile boolean isShutdown = false;

    public AlternativeSingleThreadExecutorService() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public void execute(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        taskQueue.offer(task);
    }

    private void runQueuedTasks() {
        while (!isShutdown || !taskQueue.isEmpty()) {
            try {
                taskQueue.take().run();
            } catch (InterruptedException e) {
                // taskQueue.take() throws InterruptedException even if taskQueue is not empty
                // TODO Task 2.2: terminate when appropriate
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        // TODO Task 2.1: implement shutdown method
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown && taskQueue.isEmpty() && !worker.isAlive();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO Task 2.2: implement awaitTermination method
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        // TODO Task 2.3: implement shutdownNow method
        throw new UnsupportedOperationException();
    }
}
