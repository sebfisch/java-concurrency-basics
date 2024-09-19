package sebfisch.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class Server<I, O> {
    Executor executor;
    Handler<I, O> handler;

    public Server(Handler<I, O> handler) {
        this(Executors.newCachedThreadPool(), handler);
    }

    public Server(Executor executor, Handler<I, O> handler) {
        this.executor = executor;
        this.handler = handler;
    }

    public void serve(I input, BiConsumer<O, Exception> callback) {
        executor.execute(() -> {
            try {
                callback.accept(handler.handle(input), null);
            } catch (Exception e) {
                callback.accept(null, e);
            }
        });
    }

    public CompletableFuture<O> serve(I input) {
        CompletableFuture<O> future = new CompletableFuture<>();
        serve(input, (output, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(output);
            }
        });
        return future;
    }
}
