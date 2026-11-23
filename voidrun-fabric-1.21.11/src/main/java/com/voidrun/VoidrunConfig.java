package com.voidrun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

/**
 * Settings for both loaders, stored as config/voidrun.json. Bad or missing values fall back
 * to the defaults field by field, and a broken file never throws past {@link #load(File)}.
 */
public final class VoidrunConfig {
    public static final String FILE_NAME = "voidrun.json";
    public static final String CONFIG_FOLDER = "config";

    public static final String DEFAULT_NAME = "Voidrun";
    public static final String DEFAULT_PATTERN = "{name}_{date}_{time}";
    public static final String DEFAULT_FOLDER = "voidrun";

    public static final String[] SAVE_FORMATS = {"txt", "json", "both"};
    public static final String[] SAVE_FORMAT_LABELS = {"txt", "json", "txt + json"};
    public static final String[] CORNERS = {"top_left", "top_right", "bottom_left", "bottom_right"};
    public static final String[] CORNER_LABELS = {"Top left", "Top right", "Bottom left",
            "Bottom right"};

    public static final int DURATION_MIN = 30;
    public static final int DURATION_MAX = 1800;
    public static final int DURATION_STEP = 10;
    public static final int WARMUP_MIN = 0;
    public static final int WARMUP_MAX = 30;
    public static final int SPAWN_MIN = 8;
    public static final int SPAWN_MAX = 250;
    public static final int YAW_MIN = -180;
    public static final int YAW_MAX = 180;
    public static final int PITCH_MIN = -90;
    public static final int PITCH_MAX = 90;

    public String benchmarkName = DEFAULT_NAME;
    public int durationSeconds = 300;
    public int warmupSeconds = 3;
    public String fileNamePattern = DEFAULT_PATTERN;
    public String outputFolder = DEFAULT_FOLDER;
    public String saveFormat = "both";
    public boolean autoSave = true;
    public boolean autoCopy = true;
    public int spawnHeight = 100;
    public int cameraYaw = 0;
    public int cameraPitch = 0;
    public boolean showOverlay = true;
    public String overlayCorner = "top_left";
    public boolean overlayShowTime = true;
    public boolean overlayShowFps = true;
    public boolean showGraph = true;
    public boolean confirmCancel = false;
    public boolean animations = true;

    /** Set when the file could not be read. Not written back to disk. */
    public String loadWarning = "";

    public static File fileFor(File gameDir) {
        return new File(new File(gameDir, CONFIG_FOLDER), FILE_NAME);
    }

    public VoidrunConfig copy() {
        VoidrunConfig c = new VoidrunConfig();
        c.benchmarkName = benchmarkName;
        c.durationSeconds = durationSeconds;
        c.warmupSeconds = warmupSeconds;
        c.fileNamePattern = fileNamePattern;
        c.outputFolder = outputFolder;
        c.saveFormat = saveFormat;
        c.autoSave = autoSave;
        c.autoCopy = autoCopy;
        c.spawnHeight = spawnHeight;
        c.cameraYaw = cameraYaw;
        c.cameraPitch = cameraPitch;
        c.showOverlay = showOverlay;
        c.overlayCorner = overlayCorner;
        c.overlayShowTime = overlayShowTime;
        c.overlayShowFps = overlayShowFps;
        c.showGraph = showGraph;
        c.confirmCancel = confirmCancel;
        c.animations = animations;
        return c;
    }

    public boolean sameAs(VoidrunConfig o) {
        return o != null
                && benchmarkName.equals(o.benchmarkName)
                && durationSeconds == o.durationSeconds
                && warmupSeconds == o.warmupSeconds
                && fileNamePattern.equals(o.fileNamePattern)
                && outputFolder.equals(o.outputFolder)
                && saveFormat.equals(o.saveFormat)
                && autoSave == o.autoSave
                && autoCopy == o.autoCopy
                && spawnHeight == o.spawnHeight
                && cameraYaw == o.cameraYaw
                && cameraPitch == o.cameraPitch
                && showOverlay == o.showOverlay
                && overlayCorner.equals(o.overlayCorner)
                && overlayShowTime == o.overlayShowTime
                && overlayShowFps == o.overlayShowFps
                && showGraph == o.showGraph
                && confirmCancel == o.confirmCancel
                && animations == o.animations;
    }

    // ------------------------------------------------------------------ load

    public static VoidrunConfig load(File gameDir) {
        VoidrunConfig cfg = new VoidrunConfig();
        File file = fileFor(gameDir);
        if (!file.isFile()) {
            return cfg;
        }
        try {
            Map<String, Object> map = Json.parseObject(readText(file));
            cfg.benchmarkName = str(map, "benchmark_name", cfg.benchmarkName);
            cfg.durationSeconds = num(map, "duration_seconds", cfg.durationSeconds);
            cfg.warmupSeconds = num(map, "warmup_seconds", cfg.warmupSeconds);
            cfg.fileNamePattern = str(map, "file_name_pattern", cfg.fileNamePattern);
            cfg.outputFolder = str(map, "output_folder", cfg.outputFolder);
            cfg.saveFormat = str(map, "save_format", cfg.saveFormat);
            cfg.autoSave = bool(map, "auto_save", cfg.autoSave);
            cfg.autoCopy = bool(map, "auto_copy", cfg.autoCopy);
            cfg.spawnHeight = num(map, "spawn_height", cfg.spawnHeight);
            cfg.cameraYaw = num(map, "camera_yaw", cfg.cameraYaw);
            cfg.cameraPitch = num(map, "camera_pitch", cfg.cameraPitch);
            cfg.showOverlay = bool(map, "show_overlay", cfg.showOverlay);
            cfg.overlayCorner = str(map, "overlay_corner", cfg.overlayCorner);
            cfg.overlayShowTime = bool(map, "overlay_show_time", cfg.overlayShowTime);
            cfg.overlayShowFps = bool(map, "overlay_show_fps", cfg.overlayShowFps);
            cfg.showGraph = bool(map, "show_graph", cfg.showGraph);
            cfg.confirmCancel = bool(map, "confirm_cancel", cfg.confirmCancel);
            cfg.animations = bool(map, "animations", cfg.animations);
        } catch (Throwable t) {
            cfg = new VoidrunConfig();
            cfg.loadWarning = "Config could not be read, defaults were used.";
            return cfg;
        }
        cfg.sanitize();
        return cfg;
    }

    private static String readText(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
        try {
            StringBuilder sb = new StringBuilder(1024);
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private static String str(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : fallback;
    }

    private static int num(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Double) {
            double d = ((Double) value).doubleValue();
            if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                return (int) Math.round(d);
            }
        }
        return fallback;
    }

    private static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : fallback;
    }

    // ------------------------------------------------------------------ save

    public void save(File gameDir) throws Exception {
        sanitize();
        File dir = new File(gameDir, CONFIG_FOLDER);
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new java.io.IOException("Could not create " + dir.getAbsolutePath());
        }
        File target = new File(dir, FILE_NAME);
        File temp = new File(dir, FILE_NAME + ".tmp");
        Writer writer = new OutputStreamWriter(new FileOutputStream(temp), Charset.forName("UTF-8"));
        try {
            writer.write(toJson());
            writer.flush();
        } finally {
            writer.close();
        }
        if (target.exists() && !target.delete()) {
            throw new java.io.IOException("Could not replace " + target.getAbsolutePath());
        }
        if (!temp.renameTo(target)) {
            throw new java.io.IOException("Could not write " + target.getAbsolutePath());
        }
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\n");
        sb.append("  \"benchmark_name\": \"").append(Json.escape(benchmarkName)).append("\",\n");
        sb.append("  \"duration_seconds\": ").append(durationSeconds).append(",\n");
        sb.append("  \"warmup_seconds\": ").append(warmupSeconds).append(",\n");
        sb.append("  \"file_name_pattern\": \"").append(Json.escape(fileNamePattern)).append("\",\n");
        sb.append("  \"output_folder\": \"").append(Json.escape(outputFolder)).append("\",\n");
        sb.append("  \"save_format\": \"").append(Json.escape(saveFormat)).append("\",\n");
        sb.append("  \"auto_save\": ").append(autoSave).append(",\n");
        sb.append("  \"auto_copy\": ").append(autoCopy).append(",\n");
        sb.append("  \"spawn_height\": ").append(spawnHeight).append(",\n");
        sb.append("  \"camera_yaw\": ").append(cameraYaw).append(",\n");
        sb.append("  \"camera_pitch\": ").append(cameraPitch).append(",\n");
        sb.append("  \"show_overlay\": ").append(showOverlay).append(",\n");
        sb.append("  \"overlay_corner\": \"").append(Json.escape(overlayCorner)).append("\",\n");
        sb.append("  \"overlay_show_time\": ").append(overlayShowTime).append(",\n");
        sb.append("  \"overlay_show_fps\": ").append(overlayShowFps).append(",\n");
        sb.append("  \"show_graph\": ").append(showGraph).append(",\n");
        sb.append("  \"confirm_cancel\": ").append(confirmCancel).append(",\n");
        sb.append("  \"animations\": ").append(animations).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ------------------------------------------------------------ validation

    /** Clamps numbers and normalises strings. Returns true when anything was changed. */
    public boolean sanitize() {
        boolean changed = false;
        String name = cleanName(benchmarkName);
        if (!name.equals(benchmarkName)) {
            benchmarkName = name;
            changed = true;
        }
        int duration = clamp(durationSeconds, DURATION_MIN, DURATION_MAX);
        if (duration != durationSeconds) {
            durationSeconds = duration;
            changed = true;
        }
        int warmup = clamp(warmupSeconds, WARMUP_MIN, WARMUP_MAX);
        if (warmup != warmupSeconds) {
            warmupSeconds = warmup;
            changed = true;
        }
        String pattern = patternError(fileNamePattern) == null ? fileNamePattern : DEFAULT_PATTERN;
        if (!pattern.equals(fileNamePattern)) {
            fileNamePattern = pattern;
            changed = true;
        }
        String folder = cleanFolder(outputFolder);
        if (!folder.equals(outputFolder)) {
            outputFolder = folder;
            changed = true;
        }
        String format = canonical(saveFormat, SAVE_FORMATS, "both");
        if (!format.equals(saveFormat)) {
            saveFormat = format;
            changed = true;
        }
        String corner = canonical(overlayCorner, CORNERS, "top_left");
        if (!corner.equals(overlayCorner)) {
            overlayCorner = corner;
            changed = true;
        }
        int spawn = clamp(spawnHeight, SPAWN_MIN, SPAWN_MAX);
        if (spawn != spawnHeight) {
            spawnHeight = spawn;
            changed = true;
        }
        int yaw = clamp(cameraYaw, YAW_MIN, YAW_MAX);
        if (yaw != cameraYaw) {
            cameraYaw = yaw;
            changed = true;
        }
        int pitch = clamp(cameraPitch, PITCH_MIN, PITCH_MAX);
        if (pitch != cameraPitch) {
            cameraPitch = pitch;
            changed = true;
        }
        return changed;
    }

    /** First user facing problem with the current values, or null. */
    public String validationError() {
        if (benchmarkName == null || benchmarkName.trim().length() == 0) {
            return "Benchmark name cannot be empty.";
        }
        String pattern = patternError(fileNamePattern);
        if (pattern != null) {
            return pattern;
        }
        if (cleanFolder(outputFolder).length() == 0) {
            return "Output folder cannot be empty.";
        }
        return null;
    }

    public static String patternError(String pattern) {
        if (pattern == null || pattern.trim().length() == 0) {
            return "File name pattern cannot be empty.";
        }
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end < 0) {
                    return "Unclosed { in the file name pattern.";
                }
                String token = pattern.substring(i + 1, end);
                if (!token.equals("name") && !token.equals("date") && !token.equals("time")) {
                    return "Unknown token {" + token + "} in the file name pattern.";
                }
                i = end + 1;
            } else if (c == '}') {
                return "Stray } in the file name pattern.";
            } else if (!isSafeChar(c)) {
                return "The file name pattern contains a character that cannot be saved.";
            } else {
                i++;
            }
        }
        return null;
    }

    public static String cleanName(String value) {
        String cleaned = clean(value, 40, false);
        return cleaned.length() == 0 ? DEFAULT_NAME : cleaned;
    }

    public static String cleanFolder(String value) {
        if (value == null) {
            return DEFAULT_FOLDER;
        }
        String v = value.replace('\\', '/').trim();
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '/' || (isSafeChar(c) && c != '{' && c != '}')) {
                sb.append(c);
            }
        }
        String out = sb.toString();
        while (out.startsWith("/")) {
            out = out.substring(1);
        }
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        if (out.length() > 120) {
            out = out.substring(0, 120);
        }
        String[] parts = out.split("/");
        StringBuilder safe = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0 || part.equals(".") || part.equals("..")) {
                continue;
            }
            if (safe.length() > 0) {
                safe.append('/');
            }
            safe.append(part);
        }
        return safe.length() == 0 ? DEFAULT_FOLDER : safe.toString();
    }

    private static String clean(String value, int max, boolean allowBraces) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length() && sb.length() < max; i++) {
            char c = value.charAt(i);
            if (c < 0x20) {
                continue;
            }
            if (allowBraces ? (isSafeChar(c) || c == '{' || c == '}') : isSafeChar(c)) {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        while (out.contains("  ")) {
            out = out.replace("  ", " ");
        }
        return out;
    }

    private static boolean isSafeChar(char c) {
        if (c < 0x20 || c == 0x7F) {
            return false;
        }
        return "\\/:*?\"<>|".indexOf(c) < 0;
    }

    /** Characters the settings screen accepts for the benchmark name and pattern. */
    public static boolean allowedNameChar(char c) {
        return isSafeChar(c) && c != '{' && c != '}';
    }

    public static boolean allowedFolderChar(char c) {
        return c == '/' || allowedNameChar(c);
    }

    private static String canonical(String value, String[] allowed, String fallback) {
        if (value != null) {
            String v = value.trim().toLowerCase(Locale.ROOT);
            for (String a : allowed) {
                if (a.equals(v)) {
                    return a;
                }
            }
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    // ---------------------------------------------------------------- helpers

    public boolean wantJson() {
        return "json".equals(saveFormat) || "both".equals(saveFormat);
    }

    public boolean wantTxt() {
        return "txt".equals(saveFormat) || "both".equals(saveFormat);
    }

    public String durationLabel() {
        return Ui.clock(durationSeconds);
    }

    public String formatLabel() {
        return SAVE_FORMAT_LABELS[indexOf(SAVE_FORMATS, saveFormat)];
    }

    public String cornerLabel() {
        return CORNER_LABELS[indexOf(CORNERS, overlayCorner)];
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }
}
