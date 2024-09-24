package sebfisch.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InterruptionDemo {
    public static void main(String[] args) throws InterruptedException {
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
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println(words);
    }
}
