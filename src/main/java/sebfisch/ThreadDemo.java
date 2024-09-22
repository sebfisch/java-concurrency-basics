package sebfisch;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import sebfisch.concurrent.NewThreadPerTaskExecutor;

public class ThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread hello = new Thread(() -> System.out.println("Hello"));
        hello.start();
        hello.join();

        List<Thread> threads = IntStream
                .range(0, 10)
                .mapToObj(n -> new Thread(() -> System.out.println(n)))
                .toList();

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Executor executor = new NewThreadPerTaskExecutor();
        executor.execute(() -> System.out.println("Hello"));
        IntStream.range(0, 10)
                .forEach(n -> executor.execute(() -> System.out.println(n)));
    }
}
