package sebfisch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        concurrentComputations();
        intrinsicLocking();
        racingWithInterruptions();
    }

    private static void concurrentComputations() throws InterruptedException {
        Thread helloWorld = new Thread(() -> System.out.println("Hello, World!"));
        helloWorld.start();
        helloWorld.join();

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
    }

    private static void intrinsicLocking() throws InterruptedException {
        List<String> words = new ArrayList<>();
        Thread hello = new Thread(() -> {
            synchronized (words) {
                words.add("Hello");
            }
        });
        Thread world = new Thread(() -> {
            synchronized (words) {
                words.add("World");
            }
        });
        hello.start();
        world.start();
        hello.join();
        world.join();
        System.out.println("%s, %s!".formatted(words.get(0), words.get(1)));
    }

    private static void racingWithInterruptions() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<String> words = new ArrayList<>();
        threads.add(new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (words) {
                words.add("Hello");
            }
            synchronized (threads) {
                threads.get(1).interrupt();
            }
        }));
        threads.add(new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (words) {
                words.add("World");
            }
            synchronized (threads) {
                threads.get(0).interrupt();
            }
        }));
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println(words.stream().collect(Collectors.joining(" ")));
    }
}
