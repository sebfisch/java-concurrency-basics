package sebfisch;

import java.util.List;
import java.util.stream.IntStream;

public class ThreadDemo {
    public static void main(String[] args) {
        final List<Thread> threads = IntStream.range(0, 10)
                .mapToObj(n -> new Thread(() -> {
                    System.out.println(n);
                })).toList();

        threads.forEach(Thread::start);

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
