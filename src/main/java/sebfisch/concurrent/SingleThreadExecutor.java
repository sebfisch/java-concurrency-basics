package sebfisch.concurrent;

import java.util.LinkedList;
import java.util.concurrent.Executor;

public class SingleThreadExecutor implements Executor {
    private final Thread worker;
    private final LinkedList<Runnable> taskQueue;

    public SingleThreadExecutor() {
        taskQueue = new LinkedList<>();
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public void execute(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notify();
        }
    }

    private void runQueuedTasks() {
        while (true) {
            Runnable task;
            try {
                task = getNextTask();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                task.run();
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
    }

    private Runnable getNextTask() throws InterruptedException {
        synchronized (taskQueue) {
            while (taskQueue.isEmpty()) {
                taskQueue.wait();
            }
            return taskQueue.removeFirst();
        }
    }
}
