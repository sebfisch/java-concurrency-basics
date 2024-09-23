package sebfisch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import sebfisch.concurrent.NewThreadPerTaskExecutor;

public class CompletableFutureDemo {
    public static void main(String[] args) {
        concurrentComputations();
        racingWithInterruptions();
    }

    private static void concurrentComputations() {
        NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        CompletableFuture<String> hello = executor.submit(() -> "Hello");
        CompletableFuture<String> world = executor.submit(() -> "World");
        System.out.println("%s, %s!".formatted(hello.join(), world.join()));
    }

    private static void racingWithInterruptions() {
        NewThreadPerTaskExecutor executor = new NewThreadPerTaskExecutor();
        CompletableFuture<String> hello = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(1);
            return "Hello";
        });
        CompletableFuture<String> world = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            return "World";
        });
        CompletableFuture<String> winner = new CompletableFuture<>();
        hello.whenComplete((result, error) -> {
            if (error == null) {
                winner.complete(result);
                world.cancel(false);
            } else {
                winner.completeExceptionally(error);
            }
        });
        world.whenComplete((result, error) -> {
            if (error == null) {
                winner.complete(result);
                hello.cancel(false);
            } else {
                winner.completeExceptionally(error);
            }
        });
        System.out.println(winner.join());
    }
}
