package sebfisch;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class ParallelStreamDemo {
    public static void main(String[] args) {
        printNumbersWithUsedThreads();
        comparePerformanceOfLargeSum();
        comparePrimeCheckPerformance();
    }

    private static void printNumbersWithUsedThreads() {
        IntStream.range(0, 10)
                .parallel()
                .mapToObj(n -> "%d: %s".formatted(n, Thread.currentThread().getName()))
                .forEach(System.out::println);
    }

    private static void printPerformance(String label, Supplier<?> supplier) {
        Instant start = Instant.now();
        Object result = supplier.get();
        Instant end = Instant.now();
        System.out.println("%s took: %s - result: %s"
                .formatted(label, Duration.between(start, end), result));
    }

    private static void comparePerformanceOfLargeSum() {
        long bound = 2_000_000_000;
        printPerformance("sequential sum",
                () -> LongStream.range(0, bound).sum());
        printPerformance("parallel sum",
                () -> LongStream.range(0, bound).parallel().sum());
    }

    private static void comparePrimeCheckPerformance() {
        int bound = 10_000_000;
        printPerformance("sequential prime search",
                () -> IntStream.range(0, bound)
                        .filter(ParallelStreamDemo::isPrime)
                        .count());
        printPerformance("parallel prime search",
                () -> IntStream.range(0, bound)
                        .parallel()
                        .filter(ParallelStreamDemo::isPrime)
                        .count());
    }

    private static boolean isPrime(int number) {
        if (number <= 1)
            return false;
        if (number == 2)
            return true;
        if (number % 2 == 0)
            return false;
        for (int i = 3; i <= Math.sqrt(number); i += 2) {
            if (number % i == 0)
                return false;
        }
        return true;
    }
}
