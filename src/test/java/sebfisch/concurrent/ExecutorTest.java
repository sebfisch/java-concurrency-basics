package sebfisch.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class ExecutorTest {
    @Test
    public void testNewThreadPerTaskExecutor() {
        final Executor executor = new NewThreadPerTaskExecutor();
        final Set<String> threadNames = new HashSet<>();
        IntStream.range(0, 10)
                .forEach(n -> executor.execute(() -> {
                    synchronized (threadNames) {
                        threadNames.add(Thread.currentThread().getName());
                    }
                }));
    }
}
