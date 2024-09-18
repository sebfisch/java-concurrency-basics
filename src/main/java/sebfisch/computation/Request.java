package sebfisch.computation;

public sealed interface Request {
    record Fibonacci(int argument) implements Request {
    }
}
