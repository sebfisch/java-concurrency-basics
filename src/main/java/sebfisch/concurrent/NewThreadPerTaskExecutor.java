package sebfisch.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class NewThreadPerTaskExecutor implements Executor {
    private boolean isShutdown = false;
    private final List<Thread> activeThreads = new ArrayList<>();

    @Override
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Executor has been shut down");
        }
        Thread worker = new Thread(() -> {
            try {
                task.run();
            } finally {
                synchronized (this) {
                    activeThreads.remove(Thread.currentThread());
                }
            }
        });
        activeThreads.add(worker);
        worker.start();
    }

    public synchronized void shutdownNow() {
        isShutdown = true;
        for (Thread worker : activeThreads) {
            worker.interrupt();
        }
    }
}
