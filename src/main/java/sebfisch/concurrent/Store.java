package sebfisch.concurrent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Store<D> {
    private final D data;
    private final ExecutorService executor;

    private Set<CompletableFuture<Void>> activeTasks = ConcurrentHashMap.newKeySet();

    public Store(D data) {
        this(Executors.newFixedThreadPool(4), data);
    }

    public Store(ExecutorService executor, D data) {
        this.executor = executor;
        this.data = data;
    }

    public CompletableFuture<Void> activeTasks() {
        return CompletableFuture.allOf(activeTasks.toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<Void> shutdown() {
        executor.shutdown();
        return activeTasks();
    }

    public CompletableFuture<Void> access(Consumer<D> consumer) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        activeTasks.add(future);
        future.whenComplete((result, error) -> {
            activeTasks.remove(future);
        });
        executor.execute(() -> {
            try {
                synchronized (data) {
                    consumer.accept(data);
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static void main(String[] args) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        Store<StringBuilder> store = new Store<>(new StringBuilder());
        alphabet.chars()
                .mapToObj(Character::toString)
                .forEach(chr -> {
                    store.access(data -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(
                                    ThreadLocalRandom.current().nextInt(100, 200));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        data.append(chr);
                    });
                });
        store.activeTasks().join();
        store.access(data -> System.out.println("data: %s".formatted(data)));
        store.shutdown().join();
    }
}
