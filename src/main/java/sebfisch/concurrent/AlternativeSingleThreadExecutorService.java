package sebfisch.concurrent;

import java.util.ArrayList;
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
                if (isShutdown && taskQueue.isEmpty()) {
                    break;
                }
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        if (taskQueue.isEmpty()) {
            taskQueue.offer(() -> {
                // submit empty task to wake up worker
            });
        }
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return !worker.isAlive();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        worker.join(unit.toMillis(timeout));
        return isTerminated();
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = new ArrayList<>();
        taskQueue.drainTo(remainingTasks);
        worker.interrupt();
        return remainingTasks;
    }
}
