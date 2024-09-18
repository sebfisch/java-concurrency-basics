package sebfisch.computation;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class CallbackServer {
    Executor executor = Executors.newCachedThreadPool();
    RequestHandler handler;

    CallbackServer() {
        this(new RequestHandler.Simple());
    }

    CallbackServer(RequestHandler handler) {
        this.handler = handler;
    }

    public void submit(Request request, BiConsumer<Number, Throwable> callback) {
        executor.execute(() -> {
            try {
                Number result = handler.handle(request);
                callback.accept(result, null);
            } catch (Throwable t) {
                callback.accept(null, t);
            }
        });
    }
}
