package sebfisch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureDemo {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        concurrentComputations();
        racingWithInterruptions();
    }

    private static void concurrentComputations() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread::new);
        Future<String> hello = executor.submit(() -> "Hello");
        Future<String> world = executor.submit(() -> "World");
        System.out.println("%s, %s!".formatted(hello.get(), world.get()));
    }

    private static void racingWithInterruptions() {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread::new);
        List<Future<String>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            TimeUnit.SECONDS.sleep(1);
            futures.get(1).cancel(true);
            return "Hello";
        }));
        futures.add(executor.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            futures.get(0).cancel(true);
            return "World";
        }));
        for (Future<String> future : futures) {
            try {
                System.out.println(future.get());
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
