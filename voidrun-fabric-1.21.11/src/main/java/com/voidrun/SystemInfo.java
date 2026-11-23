package com.voidrun;

/** Environment snapshot taken when a run finishes. */
public final class SystemInfo {
    public String os = "unknown";
    public String arch = "unknown";
    public String javaVersion = "unknown";
    public String javaVendor = "unknown";
    public String cpuModel = "unknown";
    public String gpuVendor = "unknown";
    public String gpuRenderer = "unknown";
    public String glVersion = "unknown";
    public int cpuCores;
    public int maxMemoryMb;
    public int width;
    public int height;
    public int guiScale;
    public int renderDistance;
}
