package sebfisch.computation;

public sealed interface Response {
    record Result(Number value) implements Response {
    }

    record Error(RuntimeException cause) implements Response {
    }
}
