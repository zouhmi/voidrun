package com.voidrun;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.FileReader;

/** Fills SystemInfo from Java, GL and the Minecraft client. */
public final class PlatformInfo {
    private PlatformInfo() {
    }

    public static void fill(SystemInfo info, Object minecraft) {
        Minecraft mc = minecraft instanceof Minecraft ? (Minecraft) minecraft : Minecraft.getInstance();
        info.os = String.valueOf(System.getProperty("os.name", "unknown")) + " "
                + String.valueOf(System.getProperty("os.version", ""));
        info.arch = System.getProperty("os.arch", "unknown");
        info.javaVersion = System.getProperty("java.version", "unknown");
        info.javaVendor = System.getProperty("java.vendor", "unknown");
        info.cpuModel = cpuModel();
        info.cpuCores = Runtime.getRuntime().availableProcessors();
        info.maxMemoryMb = (int) (Runtime.getRuntime().maxMemory() / (1024L * 1024L));
        info.gpuVendor = glString(GL11.GL_VENDOR);
        info.gpuRenderer = glString(GL11.GL_RENDERER);
        info.glVersion = glString(GL11.GL_VERSION);
        if (mc != null) {
            info.width = mc.getWindow().getWidth();
            info.height = mc.getWindow().getHeight();
            info.guiScale = mc.options.guiScale().get();
            info.renderDistance = mc.options.renderDistance().get();
        }
    }

    private static String glString(int name) {
        try {
            String s = GL11.glGetString(name);
            return s == null || s.length() == 0 ? "unknown" : s.trim();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String cpuModel() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf("model name");
                if (idx >= 0) {
                    int colon = line.indexOf(':', idx);
                    if (colon > 0) {
                        return line.substring(colon + 1).trim();
                    }
                }
            }
        } catch (Throwable ignored) {
            // not on Linux
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
        String env = System.getenv("PROCESSOR_IDENTIFIER");
        return env == null || env.length() == 0 ? "unknown" : env;
    }
}
