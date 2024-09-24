package sebfisch.demo;

import java.util.ArrayList;
import java.util.List;

public class ExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
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
