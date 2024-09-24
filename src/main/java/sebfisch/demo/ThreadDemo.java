package sebfisch.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread helloWorld = new Thread(() -> {
            try{
            TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("Interrupted!");
            }
            System.out.println("Hello, World!");
        });
        System.out.println(Thread.currentThread().isDaemon());
        System.out.println(helloWorld.isDaemon());
        helloWorld.start();

        List<Thread> threads = IntStream.range(0, 10)
            .mapToObj(i -> new Thread(() -> {
                System.out.println("Thread " + i);
            }))
            .collect(Collectors.toList());
        
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Executor executor = new NewThreadPerTaskExecutor();
        IntStream.range(0,10).forEach(n -> executor.execute(() -> {
            System.out.println("Task " + n);
        }));

        List<String> words = new ArrayList<>();
        Thread hello = new Thread(() -> {
            synchronized(words) {
                words.add("Hello");
            }
        });
        Thread world = new Thread(() -> {
            synchronized(words) {
                words.add("World");
            }
        });
        hello.start();
        world.start();
        hello.join();
        world.join();
        System.out.println(words);
    }
}
