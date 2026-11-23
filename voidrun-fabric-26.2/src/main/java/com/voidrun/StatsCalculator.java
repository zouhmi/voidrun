package com.voidrun;

import java.util.Arrays;

/** Pure statistics helpers shared by both platform implementations. */
public final class StatsCalculator {
    private StatsCalculator() {
    }

    /** Nearest-rank percentile. Returns 0 for an empty input. */
    public static double percentile(double[] values, double p) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        int idx = (int) Math.ceil(p / 100.0 * n) - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx > n - 1) {
            idx = n - 1;
        }
        return sorted[idx];
    }

    /** Bucket-averages the series down to at most maxPoints values. */
    public static double[] downsample(double[] values, int maxPoints) {
        if (values == null || values.length == 0) {
            return new double[0];
        }
        if (maxPoints < 1) {
            maxPoints = 1;
        }
        int n = values.length;
        if (n <= maxPoints) {
            return values.clone();
        }
        double[] out = new double[maxPoints];
        for (int i = 0; i < maxPoints; i++) {
            int start = (int) ((long) i * n / maxPoints);
            int end = (int) ((long) (i + 1) * n / maxPoints);
            if (end <= start) {
                end = Math.min(n, start + 1);
            }
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += values[j];
            }
            out[i] = sum / (end - start);
        }
        return out;
    }

    /** Computes the full result record from raw frame times. */
    public static ResultData compute(double[] frameTimesMs, double durationSec) {
        ResultData d = new ResultData();
        d.durationSec = durationSec;
        d.frameTimesMs = frameTimesMs == null ? new double[0] : frameTimesMs.clone();
        d.fpsSeries = new double[UiSpec.FPS_BUCKETS];

        int n = d.frameTimesMs.length;
        d.frames = n;
        if (n == 0) {
            return d;
        }

        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double v = d.frameTimesMs[i];
            sum += v;
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double avg = sum / n;
        double varSum = 0.0;
        for (int i = 0; i < n; i++) {
            double diff = d.frameTimesMs[i] - avg;
            varSum += diff * diff;
        }

        d.avgFrameMs = avg;
        d.minFrameMs = min;
        d.maxFrameMs = max;
        d.stddevFrameMs = Math.sqrt(varSum / n);
        d.avgFps = avg > 0.0 ? 1000.0 / avg : 0.0;
        d.minFps = max > 0.0 ? 1000.0 / max : 0.0;
        d.maxFps = min > 0.0 ? 1000.0 / min : 0.0;

        d.p99FrameMs = percentile(d.frameTimesMs, 99.0);
        d.p999FrameMs = percentile(d.frameTimesMs, 99.9);
        d.p1LowFps = d.p99FrameMs > 0.0 ? 1000.0 / d.p99FrameMs : 0.0;
        d.p01LowFps = d.p999FrameMs > 0.0 ? 1000.0 / d.p999FrameMs : 0.0;

        double window = durationSec > 0.0 ? durationSec : 300.0;
        double[] bucketSum = new double[UiSpec.FPS_BUCKETS];
        int[] bucketCount = new int[UiSpec.FPS_BUCKETS];
        double t = 0.0;
        for (int i = 0; i < n; i++) {
            t += d.frameTimesMs[i];
            int b = (int) Math.floor(t / window * UiSpec.FPS_BUCKETS);
            if (b < 0) {
                b = 0;
            }
            if (b > UiSpec.FPS_BUCKETS - 1) {
                b = UiSpec.FPS_BUCKETS - 1;
            }
            bucketSum[b] += d.frameTimesMs[i];
            bucketCount[b]++;
        }
        for (int b = 0; b < UiSpec.FPS_BUCKETS; b++) {
            if (bucketCount[b] > 0) {
                double avgMs = bucketSum[b] / bucketCount[b];
                d.fpsSeries[b] = avgMs > 0.0 ? 1000.0 / avgMs : 0.0;
            } else {
                d.fpsSeries[b] = 0.0;
            }
        }
        return d;
    }
}
