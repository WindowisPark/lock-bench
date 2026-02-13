package io.lockbench.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record LatencySummary(
        long p50Millis,
        long p95Millis,
        long p99Millis
) {
    public static LatencySummary of(List<Long> nanosSamples) {
        if (nanosSamples == null || nanosSamples.isEmpty()) {
            return new LatencySummary(0, 0, 0);
        }

        List<Long> sorted = new ArrayList<>(nanosSamples);
        sorted.sort(Comparator.naturalOrder());
        return new LatencySummary(
                toMillis(percentile(sorted, 0.50)),
                toMillis(percentile(sorted, 0.95)),
                toMillis(percentile(sorted, 0.99))
        );
    }

    private static long percentile(List<Long> sortedNanos, double ratio) {
        int index = (int) Math.ceil(ratio * sortedNanos.size()) - 1;
        int bounded = Math.max(0, Math.min(index, sortedNanos.size() - 1));
        return sortedNanos.get(bounded);
    }

    private static long toMillis(long nanos) {
        return nanos / 1_000_000;
    }
}
