package sebfisch.concurrent;

public interface Handler<I, O> {
    O handle(I input) throws Exception;
}
