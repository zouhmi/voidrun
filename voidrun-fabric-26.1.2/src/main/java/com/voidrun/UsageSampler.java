package com.voidrun;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/** Averages process CPU load and used heap while a run is in progress. */
public final class UsageSampler {
    private static final long MB = 1024L * 1024L;

    private Object osBean;
    private Method cpuMethod;

    private double cpuSum;
    private int cpuCount;
    private double ramSum;
    private int ramCount;

    private long lastSampleNs;
    private double lastCpuPercent;
    private double lastRamMb;

    public void reset() {
        cpuSum = 0.0;
        cpuCount = 0;
        ramSum = 0.0;
        ramCount = 0;
        lastSampleNs = 0L;
        lastCpuPercent = 0.0;
        lastRamMb = 0.0;
        resolveBean();
    }

    @SuppressWarnings("unchecked")
    private void resolveBean() {
        osBean = null;
        cpuMethod = null;
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            osBean = bean;
            Class<?> iface = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (iface.isInstance(bean)) {
                for (String name : new String[]{"getProcessCpuLoad", "getCpuLoad"}) {
                    try {
                        Method m = iface.getMethod(name);
                        m.setAccessible(true);
                        cpuMethod = m;
                        break;
                    } catch (NoSuchMethodException ignored) {
                        // try the next name
                    }
                }
            }
        } catch (Throwable ignored) {
            // CPU load reporting is best effort
        }
    }

    /** Cheap enough to call every frame; internally throttled to 200 ms. */
    public void sample(long nowNs) {
        if (lastSampleNs != 0L && nowNs - lastSampleNs < 200000000L) {
            return;
        }
        lastSampleNs = nowNs;

        if (cpuMethod != null && osBean != null) {
            try {
                Object value = cpuMethod.invoke(osBean);
                if (value instanceof Double) {
                    double load = ((Double) value).doubleValue();
                    if (load >= 0.0 && load <= 1.0) {
                        lastCpuPercent = load * 100.0;
                        cpuSum += lastCpuPercent;
                        cpuCount++;
                    }
                }
            } catch (Throwable ignored) {
                // ignore broken bean
            }
        }

        Runtime rt = Runtime.getRuntime();
        lastRamMb = (rt.totalMemory() - rt.freeMemory()) / (double) MB;
        ramSum += lastRamMb;
        ramCount++;
    }

    public double lastCpuPercent() {
        return lastCpuPercent;
    }

    public double lastRamMb() {
        return lastRamMb;
    }

    public double avgCpuPercent() {
        return cpuCount == 0 ? 0.0 : cpuSum / cpuCount;
    }

    public double avgRamMb() {
        return ramCount == 0 ? 0.0 : ramSum / ramCount;
    }
}
