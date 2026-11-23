package com.voidrun;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One completed benchmark run. The field set is identical in both mods. */
public final class ResultData {
    public String runId = "";
    public String benchmarkName = VoidrunConfig.DEFAULT_NAME;
    public long timestampEpochMs;
    public String modVersion = "1.1.0";
    public String mcVersion = "unknown";
    public String loader = "unknown";
    public String loaderVersion = "unknown";
    public double durationSec = 300.0;
    public int frames;
    public double avgFps;
    public double minFps;
    public double maxFps;
    public double p1LowFps;
    public double p01LowFps;
    public double avgFrameMs;
    public double minFrameMs;
    public double maxFrameMs;
    public double p99FrameMs;
    public double p999FrameMs;
    public double stddevFrameMs;
    public double avgCpuPercent;
    public double avgRamMb;
    public boolean vsyncOn;
    public int fpsLimit;
    public boolean windowFocusLost;
    public double[] fpsSeries = new double[UiSpec.FPS_BUCKETS];
    public double[] frameTimesMs = new double[0];
    public SystemInfo system = new SystemInfo();
    /** Extra warnings collected during the run, e.g. a missing results folder. */
    public final List<String> notes = new ArrayList<String>();

    public String tierLabel() {
        if (avgFps >= 240.0) {
            return "Excellent";
        }
        if (avgFps >= 144.0) {
            return "Great";
        }
        if (avgFps >= 90.0) {
            return "Good";
        }
        if (avgFps >= 60.0) {
            return "Fair";
        }
        return "Poor";
    }

    /** ARGB colour matching the tier. */
    public int tierColor() {
        if (avgFps >= 240.0) {
            return UiSpec.TIER_EXCELLENT;
        }
        if (avgFps >= 144.0) {
            return UiSpec.TIER_GREAT;
        }
        if (avgFps >= 90.0) {
            return UiSpec.TIER_GOOD;
        }
        if (avgFps >= 60.0) {
            return UiSpec.TIER_FAIR;
        }
        return UiSpec.TIER_POOR;
    }

    /** Duration as m:ss, e.g. 5:00. */
    public String durationLabel() {
        return Ui.clock(durationSec);
    }

    /** Things the reader has to know before comparing the numbers. */
    public List<String> warnings() {
        List<String> out = new ArrayList<String>(notes);
        if (frames == 0) {
            out.add("No frames were recorded.");
        }
        if (vsyncOn) {
            out.add("VSync was on, so the frame rate may be capped by the display.");
        }
        if (fpsLimit > 0) {
            out.add("The game frame rate limit was " + fpsLimit + " fps.");
        }
        if (windowFocusLost) {
            out.add("The window lost focus during the run, so these numbers may be lower.");
        }
        return out;
    }

    /** Plain text summary copied to the clipboard. */
    public String summaryText() {
        StringBuilder sb = new StringBuilder(220);
        sb.append(benchmarkName).append(": ").append(fmt1(avgFps)).append(" avg fps (")
                .append(tierLabel().toLowerCase(Locale.ROOT)).append(")\n");
        sb.append(durationLabel()).append(" run | 1% low ").append(fmt1(p1LowFps))
                .append(" | 0.1% low ").append(fmt1(p01LowFps))
                .append(" | avg frame ").append(fmt2(avgFrameMs)).append(" ms\n");
        sb.append("CPU ").append(fmt1(avgCpuPercent)).append("% | RAM ").append(fmt0(avgRamMb))
                .append(" MB\n");
        sb.append(mcVersion).append(' ').append(loader).append(" | ").append(safe(system.cpuModel))
                .append(" | ").append(safe(system.gpuRenderer)).append('\n');
        if (runId.length() > 0) {
            sb.append("Saved: ").append(runId).append(".json");
        } else {
            sb.append("Results were not saved.");
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null || s.length() == 0 ? "unknown" : s;
    }

    private static String fmt1(double v) {
        return Ui.fmt(v, 1);
    }

    private static String fmt2(double v) {
        return Ui.fmt(v, 2);
    }

    private static String fmt0(double v) {
        return Ui.fmt(v, 0);
    }
}
