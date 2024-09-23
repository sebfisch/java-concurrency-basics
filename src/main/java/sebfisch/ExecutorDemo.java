package sebfisch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import sebfisch.concurrent.NewThreadPerTaskExecutor;

public class ExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
        concurrentComputations();
        intrinsicLocking();
    }

    private static void concurrentComputations() {
        Executor executor = new NewThreadPerTaskExecutor();
        executor.execute(() -> System.out.println("Hello"));
        IntStream.range(0, 10)
                .forEach(n -> executor.execute(() -> System.out.println(n)));
    }

    private static void intrinsicLocking() throws InterruptedException {
        NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        List<String> words = new ArrayList<>();
        executor.execute(() -> {
            synchronized (words) {
                words.add("Hello");
            }
        });
        executor.execute(() -> {
            synchronized (words) {
                words.add("World");
            }
        });
        executor.shutdown();
        executor.awaitTermination();
        System.out.println("%s, %s!".formatted(words.get(0), words.get(1)));
    }
}
