package sebfisch.computation;

public interface RequestHandler {
    Number handle(Request request);

    static final class Simple implements RequestHandler {
        @Override
        public Number handle(Request request) {
            return switch (request) {
                case Request.Fibonacci(int n) -> {
                    if (n < 0) {
                        throw new IllegalArgumentException("negative argument");
                    }
                    if (n <= 1) {
                        yield n;
                    }
                    long a = 0;
                    long b = 1;
                    for (int i = 2; i <= n; i++) {
                        long c = a + b;
                        a = b;
                        b = c;
                    }
                    yield b;
                }
            };
        }
    }
}
