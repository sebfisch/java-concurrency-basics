package sebfisch.demo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingleThreadExecutorService extends AbstractExecutorService {
    private Thread worker;
    private final LinkedList<Runnable> taskQueue = new LinkedList<>();
    private boolean isShutdown = false;

    private final Lock lock = new ReentrantLock();
    private final Condition terminated = lock.newCondition();
    private final Condition taskAvailable = lock.newCondition();

    public SingleThreadExecutorService() {
        worker = new Thread(this::runQueuedTasks);
        worker.start();
    }

    @Override
    public void execute(Runnable command) {
        lock.lock();
        try {
            if (isShutdown) {
                throw new RejectedExecutionException("Executor has been shut down");
            }
            taskQueue.addLast(command);
            taskAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    private void runQueuedTasks() {
        while (true) {
            try {
                getQueuedTask().run();
            } catch (InterruptedException e) {
                if (isShutdown()) {
                    break;
                }
            } catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
        lock.lock();
        try {
            terminated.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private Runnable getQueuedTask() throws InterruptedException {
        lock.lock();
        try {
            while (taskQueue.isEmpty()) {
                if (isShutdown) {
                    throw new InterruptedException();
                }
                taskAvailable.await();
            }
            return taskQueue.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.lock();
        try {
            isShutdown = true;
            taskAvailable.signal();
            if (isTerminated()) {
                terminated.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        lock.lock();
        try {
            return isShutdown;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        lock.lock();
        try {
            isShutdown = true;
            final List<Runnable> remainingTasks = new ArrayList<>(taskQueue);
            taskQueue.clear();
            worker.interrupt();
            return remainingTasks;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isTerminated() {
        lock.lock();
        try {
            return isShutdown && taskQueue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (!isTerminated() && nanos > 0) {
                nanos = terminated.awaitNanos(nanos);
            }
            return isTerminated();
        } finally {
            lock.unlock();
        }
    }
}
