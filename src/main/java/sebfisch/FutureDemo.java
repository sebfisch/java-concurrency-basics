package sebfisch;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureDemo {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread::new);

        Future<String> one = executor.submit(() -> "Hello, World!");
        Future<Boolean> two = executor.submit(() -> true);
        if (two.get()) {
            System.out.println(one.get());
        }

        Future<Void> three = executor.submit(() -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        });
        three.cancel(true);
        try {
            three.get();
        } catch (CancellationException e) {
            System.out.println("cancelled");
        }
    }
}
