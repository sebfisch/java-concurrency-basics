package sebfisch.computation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncedServer {
    RequestHandler handler;
    Map<UUID, Response> responses;

    SyncedServer() {
        this(new RequestHandler.Simple(), new HashMap<>());
    }

    SyncedServer(RequestHandler handler, Map<UUID, Response> responses) {
        this.handler = handler;
        this.responses = responses;
    }

    public UUID submit(Request request) {
        UUID id = UUID.randomUUID();
        new Thread(() -> {
            try {
                synchronized (responses) {
                    responses.put(id, new Response.Result(handler.handle(request)));
                    responses.notifyAll();
                }
            } catch (RuntimeException e) {
                synchronized (responses) {
                    responses.put(id, new Response.Error(e));
                    responses.notifyAll();
                }
            }
        }).start();
        return id;
    }

    public Number get(UUID id) {
        while (!responses.containsKey(id)) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        switch (responses.remove(id)) {
            case Response.Result(Number value) -> {
                return value;
            }
            case Response.Error(RuntimeException cause) -> {
                throw cause;
            }
        }
    }
}
