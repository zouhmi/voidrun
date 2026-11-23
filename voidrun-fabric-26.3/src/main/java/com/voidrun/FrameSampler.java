package com.voidrun;

/**
 * Samples rendered frames with System.nanoTime(), one call per rendered frame. The buffer is a
 * plain double array so a long run does not create one boxed object per frame.
 */
public final class FrameSampler {
    private static final double MAX_ACCEPTED_MS = 5000.0;
    private static final int MAX_SAMPLES = 2000000;

    private double[] samples = new double[8192];
    private int count;
    private long baseNs;
    private long lastNs;
    private boolean finished;

    public void reset() {
        if (samples.length > 65536) {
            samples = new double[8192];
        }
        count = 0;
        baseNs = 0L;
        lastNs = 0L;
        finished = false;
    }

    /** Must be called once per rendered frame, before the window is closed. */
    public void onFrame(long nowNs) {
        if (finished) {
            return;
        }
        if (lastNs == 0L) {
            baseNs = nowNs;
            lastNs = nowNs;
            return;
        }
        double deltaMs = (nowNs - lastNs) / 1.0e6;
        if (deltaMs <= 0.0 || deltaMs > MAX_ACCEPTED_MS) {
            lastNs = nowNs;
            return;
        }
        if (count < MAX_SAMPLES) {
            if (count == samples.length) {
                double[] bigger = new double[samples.length * 2];
                System.arraycopy(samples, 0, bigger, 0, samples.length);
                samples = bigger;
            }
            samples[count++] = deltaMs;
        }
        lastNs = nowNs;
    }

    /** Closes the window and returns the samples. Frames after this call are ignored. */
    public double[] finish(long nowNs) {
        finished = true;
        if (lastNs != 0L && count > 0 && elapsedSec(nowNs) <= windowSec()) {
            double deltaMs = (nowNs - lastNs) / 1.0e6;
            if (deltaMs > 0.0 && deltaMs <= MAX_ACCEPTED_MS && count < MAX_SAMPLES) {
                if (count == samples.length) {
                    double[] bigger = new double[samples.length * 2];
                    System.arraycopy(samples, 0, bigger, 0, samples.length);
                    samples = bigger;
                }
                samples[count++] = deltaMs;
                lastNs = nowNs;
            }
        }
        double[] out = new double[count];
        System.arraycopy(samples, 0, out, 0, count);
        return out;
    }

    /** Window length of the current run, set by RunManager so the sampler stays platform free. */
    private double windowSec = 300.0;

    public void setWindowSec(double seconds) {
        windowSec = seconds > 0.0 ? seconds : 300.0;
    }

    private double windowSec() {
        return windowSec;
    }

    public int count() {
        return count;
    }

    /** Seconds since the first frame after reset(). */
    public double elapsedSec(long nowNs) {
        if (baseNs == 0L) {
            return 0.0;
        }
        return (nowNs - baseNs) / 1.0e9;
    }

    public boolean hasBase() {
        return baseNs != 0L;
    }
}
