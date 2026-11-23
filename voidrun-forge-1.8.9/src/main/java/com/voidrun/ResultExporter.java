package com.voidrun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Writes results under the configured folder as .txt and/or .json. */
public final class ResultExporter {
    private ResultExporter() {
    }

    public static File resolveFolder(File gameDir, String configured) {
        String folder = VoidrunConfig.cleanFolder(configured);
        return new File(gameDir, folder);
    }

    /**
     * Writes the enabled files. Returns the file that was written last (json when both are on),
     * or null when the configured format was disabled.
     */
    public static File save(File gameDir, VoidrunConfig cfg, ResultData data) throws Exception {
        File dir = resolveFolder(gameDir, cfg.outputFolder);
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new java.io.IOException("Could not create " + dir.getAbsolutePath());
        }
        long ms = data.timestampEpochMs > 0L ? data.timestampEpochMs : System.currentTimeMillis();
        String base = uniqueBase(dir, filenameBase(cfg, ms));
        data.runId = base;
        Charset utf8 = Charset.forName("UTF-8");
        File written = null;
        if (cfg.wantTxt()) {
            File txt = new File(dir, base + ".txt");
            writeFile(txt, toText(data, cfg), utf8);
            written = txt;
        }
        if (cfg.wantJson()) {
            File json = new File(dir, base + ".json");
            writeFile(json, toJson(data, cfg), utf8);
            written = json;
        }
        return written;
    }

    private static String uniqueBase(File dir, String base) {
        String candidate = base;
        int index = 2;
        while (new File(dir, candidate + ".json").exists()
                || new File(dir, candidate + ".txt").exists()) {
            candidate = base + "-" + index;
            index++;
            if (index > 999) {
                break;
            }
        }
        return candidate;
    }

    private static void writeFile(File file, String content, Charset charset) throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        try {
            Writer writer = new OutputStreamWriter(out, charset);
            writer.write(content);
            writer.flush();
        } finally {
            try {
                out.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    /** Expands the configured pattern. Illegal characters are removed afterwards. */
    public static String filenameBase(VoidrunConfig cfg, long epochMs) {
        String name = sanitizeFileName(cfg.benchmarkName);
        if (name.length() == 0) {
            name = VoidrunConfig.DEFAULT_NAME;
        }
        String date = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date(epochMs));
        String time = new SimpleDateFormat("HHmmss", Locale.ROOT).format(new Date(epochMs));
        String base = cfg.fileNamePattern
                .replace("{name}", name)
                .replace("{date}", date)
                .replace("{time}", time);
        base = sanitizeFileName(base);
        if (base.length() == 0) {
            base = "voidrun_" + date + "_" + time;
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base;
    }

    /** Removes characters that cannot appear in a file name on any of the supported systems. */
    public static String sanitizeFileName(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || "\\/:*?\"<>|".indexOf(c) >= 0) {
                continue;
            }
            sb.append(c);
        }
        String out = sb.toString().trim();
        while (out.endsWith(".")) {
            out = out.substring(0, out.length() - 1);
        }
        while (out.contains("  ")) {
            out = out.replace("  ", " ");
        }
        return out;
    }

    public static String isoUtc(long epochMs) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochMs));
    }

    public static String toJson(ResultData d, VoidrunConfig cfg) {
        StringBuilder sb = new StringBuilder(16384);
        SystemInfo s = d.system == null ? new SystemInfo() : d.system;
        sb.append("{\n");
        sb.append("  \"run_id\": \"").append(Json.escape(d.runId)).append("\",\n");
        sb.append("  \"benchmark_name\": \"").append(Json.escape(d.benchmarkName)).append("\",\n");
        sb.append("  \"timestamp\": \"").append(isoUtc(d.timestampEpochMs)).append("\",\n");
        sb.append("  \"mod\": { \"id\": \"voidrun\", \"name\": \"Voidrun\", \"version\": \"")
                .append(Json.escape(d.modVersion)).append("\" },\n");
        sb.append("  \"game\": {\n");
        sb.append("    \"mc_version\": \"").append(Json.escape(d.mcVersion)).append("\",\n");
        sb.append("    \"loader\": \"").append(Json.escape(d.loader)).append("\",\n");
        sb.append("    \"loader_version\": \"").append(Json.escape(d.loaderVersion)).append("\",\n");
        sb.append("    \"render_distance\": ").append(s.renderDistance).append(",\n");
        sb.append("    \"gui_scale\": ").append(s.guiScale).append(",\n");
        sb.append("    \"resolution\": \"").append(s.width).append('x').append(s.height).append("\",\n");
        sb.append("    \"vsync\": ").append(d.vsyncOn).append(",\n");
        sb.append("    \"frame_limit\": ").append(d.fpsLimit).append('\n');
        sb.append("  },\n");
        sb.append("  \"settings\": {\n");
        sb.append("    \"duration_seconds\": ").append(cfg.durationSeconds).append(",\n");
        sb.append("    \"warmup_seconds\": ").append(cfg.warmupSeconds).append(",\n");
        sb.append("    \"spawn_height\": ").append(cfg.spawnHeight).append(",\n");
        sb.append("    \"camera_yaw\": ").append(cfg.cameraYaw).append(",\n");
        sb.append("    \"camera_pitch\": ").append(cfg.cameraPitch).append('\n');
        sb.append("  },\n");
        sb.append("  \"system\": {\n");
        sb.append("    \"os\": \"").append(Json.escape(s.os)).append("\",\n");
        sb.append("    \"arch\": \"").append(Json.escape(s.arch)).append("\",\n");
        sb.append("    \"java_version\": \"").append(Json.escape(s.javaVersion)).append("\",\n");
        sb.append("    \"java_vendor\": \"").append(Json.escape(s.javaVendor)).append("\",\n");
        sb.append("    \"cpu_model\": \"").append(Json.escape(s.cpuModel)).append("\",\n");
        sb.append("    \"cpu_cores\": ").append(s.cpuCores).append(",\n");
        sb.append("    \"max_memory_mb\": ").append(s.maxMemoryMb).append(",\n");
        sb.append("    \"gpu_vendor\": \"").append(Json.escape(s.gpuVendor)).append("\",\n");
        sb.append("    \"gpu_renderer\": \"").append(Json.escape(s.gpuRenderer)).append("\",\n");
        sb.append("    \"gl_version\": \"").append(Json.escape(s.glVersion)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"results\": {\n");
        sb.append("    \"duration_sec\": ").append(num(d.durationSec)).append(",\n");
        sb.append("    \"frames\": ").append(d.frames).append(",\n");
        sb.append("    \"avg_fps\": ").append(num(d.avgFps)).append(",\n");
        sb.append("    \"min_fps\": ").append(num(d.minFps)).append(",\n");
        sb.append("    \"max_fps\": ").append(num(d.maxFps)).append(",\n");
        sb.append("    \"p1_low_fps\": ").append(num(d.p1LowFps)).append(",\n");
        sb.append("    \"p01_low_fps\": ").append(num(d.p01LowFps)).append(",\n");
        sb.append("    \"avg_frame_ms\": ").append(num(d.avgFrameMs)).append(",\n");
        sb.append("    \"min_frame_ms\": ").append(num(d.minFrameMs)).append(",\n");
        sb.append("    \"max_frame_ms\": ").append(num(d.maxFrameMs)).append(",\n");
        sb.append("    \"p99_frame_ms\": ").append(num(d.p99FrameMs)).append(",\n");
        sb.append("    \"p999_frame_ms\": ").append(num(d.p999FrameMs)).append(",\n");
        sb.append("    \"stddev_frame_ms\": ").append(num(d.stddevFrameMs)).append(",\n");
        sb.append("    \"avg_cpu_percent\": ").append(num(d.avgCpuPercent)).append(",\n");
        sb.append("    \"avg_ram_mb\": ").append(num(d.avgRamMb)).append('\n');
        sb.append("  },\n");
        sb.append("  \"warnings\": [");
        appendStrings(sb, d.warnings());
        sb.append("],\n");
        sb.append("  \"fps_series\": [");
        appendArrayBody(sb, d.fpsSeries);
        sb.append("],\n");
        sb.append("  \"frame_times_ms\": [");
        appendArrayBody(sb, d.frameTimesMs);
        sb.append("]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** Appends the array body (no brackets). Empty arrays stay on one line. */
    private static void appendArrayBody(StringBuilder sb, double[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i < values.length; i++) {
            sb.append("    ").append(num(values[i]));
            if (i < values.length - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ");
    }

    private static void appendStrings(StringBuilder sb, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i < values.size(); i++) {
            sb.append("    \"").append(Json.escape(values.get(i))).append('"');
            if (i < values.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ");
    }

    public static String toText(ResultData d, VoidrunConfig cfg) {
        SystemInfo s = d.system == null ? new SystemInfo() : d.system;
        StringBuilder sb = new StringBuilder(2048);
        sb.append("VOIDRUN RESULT\n");
        sb.append("==============\n");
        sb.append("Benchmark:   ").append(d.benchmarkName).append('\n');
        sb.append("Run ID:      ").append(d.runId).append('\n');
        sb.append("Date:        ").append(isoUtc(d.timestampEpochMs)).append('\n');
        sb.append("Game:        ").append(d.mcVersion).append(' ').append(d.loader).append(' ')
                .append(d.loaderVersion).append('\n');
        sb.append("System:      ").append(s.os).append(" / ").append(s.cpuModel).append(" / ")
                .append(s.gpuRenderer).append('\n');
        sb.append("Resolution:  ").append(s.width).append('x').append(s.height)
                .append(" (gui scale ").append(s.guiScale).append(", render distance ")
                .append(s.renderDistance).append(")\n");
        sb.append("Java:        ").append(s.javaVersion).append(" (").append(s.javaVendor)
                .append(")\n");
        sb.append("Settings:    ").append(cfg.durationLabel()).append(" run, ")
                .append(cfg.warmupSeconds).append(" s warm-up, spawn height ")
                .append(cfg.spawnHeight).append(", yaw ").append(cfg.cameraYaw)
                .append(", pitch ").append(cfg.cameraPitch).append('\n');
        sb.append('\n');
        sb.append("FRAME RATE\n");
        sb.append("----------\n");
        sb.append(row("Average FPS:", num3(d.avgFps))).append("  (").append(d.tierLabel())
                .append(")\n");
        sb.append(row("Minimum FPS:", num3(d.minFps))).append('\n');
        sb.append(row("Maximum FPS:", num3(d.maxFps))).append('\n');
        sb.append(row("1% low FPS:", num3(d.p1LowFps))).append('\n');
        sb.append(row("0.1% low FPS:", num3(d.p01LowFps))).append('\n');
        sb.append('\n');
        sb.append("FRAME TIME\n");
        sb.append("----------\n");
        sb.append(row("Average:", num3(d.avgFrameMs) + " ms")).append('\n');
        sb.append(row("Minimum:", num3(d.minFrameMs) + " ms")).append('\n');
        sb.append(row("Maximum:", num3(d.maxFrameMs) + " ms")).append('\n');
        sb.append(row("P99:", num3(d.p99FrameMs) + " ms")).append('\n');
        sb.append(row("P99.9:", num3(d.p999FrameMs) + " ms")).append('\n');
        sb.append(row("Std dev:", num3(d.stddevFrameMs) + " ms")).append('\n');
        sb.append(row("Frames:", d.frames + " in "
                + String.format(Locale.ROOT, "%.1f", Double.valueOf(d.durationSec)) + " s"))
                .append('\n');
        sb.append('\n');
        sb.append("SYSTEM LOAD\n");
        sb.append("-----------\n");
        sb.append(row("Avg CPU:", num3(d.avgCpuPercent) + " %")).append('\n');
        sb.append(row("Avg RAM:", num3(d.avgRamMb) + " MB")).append('\n');
        List<String> warnings = d.warnings();
        if (!warnings.isEmpty()) {
            sb.append('\n');
            sb.append("WARNINGS\n");
            sb.append("--------\n");
            for (String warning : warnings) {
                sb.append("- ").append(warning).append('\n');
            }
        }
        return sb.toString();
    }

    /** label + spacing so the value ends at column 18 (matching the old sample layout). */
    private static String row(String label, String value) {
        int pad = 18 - value.length() - label.length();
        if (pad < 1) {
            pad = 1;
        }
        StringBuilder sb = new StringBuilder(label.length() + pad + value.length());
        sb.append(label);
        for (int i = 0; i < pad; i++) {
            sb.append(' ');
        }
        sb.append(value);
        return sb.toString();
    }

    public static String clipboardSummary(ResultData d) {
        return d.summaryText();
    }

    public static String num(double v) {
        return String.format(Locale.ROOT, "%.3f", Double.valueOf(v));
    }

    private static String num3(double v) {
        return num(v);
    }
}
