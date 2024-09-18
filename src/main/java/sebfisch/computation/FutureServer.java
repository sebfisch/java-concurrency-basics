package sebfisch.computation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FutureServer {
    Executor executor = Executors.newCachedThreadPool();
    RequestHandler handler;

    FutureServer() {
        this(new RequestHandler.Simple());
    }

    FutureServer(RequestHandler handler) {
        this.handler = handler;
    }

    public CompletableFuture<Number> submit(Request request) {
        CompletableFuture<Number> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                Number result = handler.handle(request);
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
