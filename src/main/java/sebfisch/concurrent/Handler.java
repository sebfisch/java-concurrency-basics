package sebfisch.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Handler<I, O> {
    O handle(I input) throws Exception;

    static <I, O> Handler<I, O> of(Function<I, O> function) {
        return function::apply;
    }

    static <I> Handler<I, Void> of(Consumer<I> consumer) {
        return input -> {
            consumer.accept(input);
            return null;
        };
    }

    static <O> Handler<Void, O> of(Callable<O> callable) {
        return ignored -> callable.call();
    }
}
