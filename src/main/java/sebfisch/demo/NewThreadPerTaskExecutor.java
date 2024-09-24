package sebfisch.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class NewThreadPerTaskExecutor implements Executor {
    List<Thread> activeWorkers = new ArrayList<>();
    private boolean isShutdown = false;

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor is shut down");
        }
        Thread worker = new Thread(() -> runTask(task));
        activeWorkers.add(worker);
        worker.start();
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
