package sebfisch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CompletableFutureDemo {
    public static void main(String[] args) {
        concurrentComputations();
        racingWithoutInterruptions();
    }

    private static void concurrentComputations() {
        CompletableFuture<String> hello = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> world = CompletableFuture.supplyAsync(() -> "World");
        CompletableFuture.allOf(hello, world).join();
        System.out.println("%s, %s!".formatted(hello.join(), world.join()));
    }

    private static void racingWithoutInterruptions() {
        CompletableFuture<String> hello = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Hello";
        });
        CompletableFuture<String> world = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "World";
        });
        CompletableFuture<Object> winner = CompletableFuture.anyOf(hello, world);
        System.out.println(winner.join());
    }
}
