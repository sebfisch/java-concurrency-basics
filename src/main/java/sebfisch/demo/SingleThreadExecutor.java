package sebfisch.demo;

import java.util.LinkedList;
import java.util.concurrent.Executor;

public class SingleThreadExecutor implements Executor {
    private final Thread worker;
    private final LinkedList<Runnable> taskQueue = new LinkedList<>();

    public SingleThreadExecutor() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public synchronized void execute(Runnable task) {
        taskQueue.addLast(task);
        notifyAll();
    }

    private void runQueuedTasks() {
        while (true) {
            try {
                getQueuedTask().run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized Runnable getQueuedTask() throws InterruptedException {
        while (taskQueue.isEmpty()) {
            wait();
        }
        return taskQueue.removeFirst();
    }
}
