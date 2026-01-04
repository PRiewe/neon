package neon.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Performance measurement harness for integration into unit tests.
 *
 * <p>Provides simple timing utilities for measuring operation performance without requiring an
 * external benchmarking framework.
 */
public class PerformanceHarness {

  /** Result of a measured operation, including the result value and timing information. */
  public static class MeasuredResult<T> {
    private final T result;
    private final long durationNanos;
    private final long durationMillis;
    private final long startTime;
    private final long endTime;

    public MeasuredResult(T result, long startTime, long endTime) {
      this.result = result;
      this.startTime = startTime;
      this.endTime = endTime;
      this.durationNanos = endTime - startTime;
      this.durationMillis = durationNanos / 1_000_000;
    }

    public T getResult() {
      return result;
    }

    public long getDurationNanos() {
      return durationNanos;
    }

    public long getDurationMillis() {
      return durationMillis;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getEndTime() {
      return endTime;
    }

    @Override
    public String toString() {
      return String.format("MeasuredResult[duration=%d ms (%d ns)]", durationMillis, durationNanos);
    }
  }

  /** Statistical summary of multiple measurements. */
  public static class Stats {
    private final long min;
    private final long max;
    private final long avg;
    private final long median;
    private final int count;

    public Stats(long min, long max, long avg, long median, int count) {
      this.min = min;
      this.max = max;
      this.avg = avg;
      this.median = median;
      this.count = count;
    }

    public long getMin() {
      return min;
    }

    public long getMax() {
      return max;
    }

    public long getAvg() {
      return avg;
    }

    public long getMedian() {
      return median;
    }

    public int getCount() {
      return count;
    }

    @Override
    public String toString() {
      return String.format(
          "Stats[count=%d, min=%d ms, max=%d ms, avg=%d ms, median=%d ms]",
          count, min, max, avg, median);
    }
  }

  /**
   * Measures the execution time of a callable operation.
   *
   * @param operation the operation to measure
   * @param <T> the return type of the operation
   * @return a MeasuredResult containing both the result and timing information
   * @throws Exception if the operation throws an exception
   */
  public static <T> MeasuredResult<T> measure(Callable<T> operation) throws Exception {
    long startTime = System.nanoTime();
    T result = operation.call();
    long endTime = System.nanoTime();
    return new MeasuredResult<>(result, startTime, endTime);
  }

  /**
   * Measures the execution time of a runnable operation.
   *
   * @param operation the operation to measure
   * @return a MeasuredResult with null result and timing information
   */
  public static MeasuredResult<Void> measure(Runnable operation) {
    long startTime = System.nanoTime();
    operation.run();
    long endTime = System.nanoTime();
    return new MeasuredResult<>(null, startTime, endTime);
  }

  /**
   * Measures the execution time of an operation with JVM warmup runs.
   *
   * <p>Runs the operation multiple times to allow JIT compilation and optimization before the
   * actual measurement. This provides more stable and representative performance numbers.
   *
   * @param operation the operation to measure
   * @param warmupRuns number of warmup executions before measurement
   * @param <T> the return type of the operation
   * @return a MeasuredResult containing the result from the measured run and timing information
   * @throws Exception if the operation throws an exception
   */
  public static <T> MeasuredResult<T> measureWithWarmup(Callable<T> operation, int warmupRuns)
      throws Exception {
    // Warmup runs
    for (int i = 0; i < warmupRuns; i++) {
      operation.call();
    }

    // Measured run
    return measure(operation);
  }

  /**
   * Measures an operation multiple times and returns statistics.
   *
   * @param operation the operation to measure
   * @param iterations number of times to run the operation
   * @param <T> the return type of the operation
   * @return statistics of the measurements (in milliseconds)
   * @throws Exception if the operation throws an exception
   */
  public static <T> Stats measureMultiple(Callable<T> operation, int iterations) throws Exception {
    List<Long> timings = new ArrayList<>(iterations);

    for (int i = 0; i < iterations; i++) {
      MeasuredResult<T> result = measure(operation);
      timings.add(result.getDurationMillis());
    }

    return computeStats(timings);
  }

  /**
   * Computes statistical summary from a list of measurements.
   *
   * @param measurements list of timing measurements in milliseconds
   * @return statistical summary
   */
  public static Stats computeStats(List<Long> measurements) {
    if (measurements.isEmpty()) {
      return new Stats(0, 0, 0, 0, 0);
    }

    List<Long> sorted = new ArrayList<>(measurements);
    sorted.sort(Long::compareTo);

    long min = sorted.get(0);
    long max = sorted.get(sorted.size() - 1);
    long sum = sorted.stream().mapToLong(Long::longValue).sum();
    long avg = sum / sorted.size();
    long median = sorted.get(sorted.size() / 2);

    return new Stats(min, max, avg, median, sorted.size());
  }

  /**
   * Formats a duration in nanoseconds to a human-readable string.
   *
   * @param nanos duration in nanoseconds
   * @return formatted string (e.g., "123 ms", "456 μs", "789 ns")
   */
  public static String formatDuration(long nanos) {
    if (nanos >= 1_000_000_000) {
      return String.format("%.2f s", nanos / 1_000_000_000.0);
    } else if (nanos >= 1_000_000) {
      return String.format("%.2f ms", nanos / 1_000_000.0);
    } else if (nanos >= 1_000) {
      return String.format("%.2f μs", nanos / 1_000.0);
    } else {
      return nanos + " ns";
    }
  }

  /**
   * Prints performance results in a formatted way.
   *
   * @param label descriptive label for the operation
   * @param result the measured result to print
   */
  public static void printResult(String label, MeasuredResult<?> result) {
    System.out.printf("[PERF] %s: %s%n", label, formatDuration(result.getDurationNanos()));
  }

  /**
   * Prints statistics in a formatted way.
   *
   * @param label descriptive label for the operations
   * @param stats the statistics to print
   */
  public static void printStats(String label, Stats stats) {
    System.out.printf(
        "[PERF] %s: min=%d ms, max=%d ms, avg=%d ms, median=%d ms (n=%d)%n",
        label, stats.min, stats.max, stats.avg, stats.median, stats.count);
  }
}
