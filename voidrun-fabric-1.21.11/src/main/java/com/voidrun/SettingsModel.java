package com.voidrun;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Working copy of the settings for the settings screen. Holds the row list, the value text and
 * the editing rules so both loaders share the same behaviour.
 */
public final class SettingsModel {
    public enum Kind {
        TEXT, SLIDER, TOGGLE, CYCLE
    }

    public static final class Row {
        public final String key;
        public final String label;
        public final String help;
        public final Kind kind;
        public final int min;
        public final int max;
        public final int step;
        public final String[] options;
        public final int maxLength;

        Row(String key, String label, String help, Kind kind, int min, int max, int step,
            String[] options, int maxLength) {
            this.key = key;
            this.label = label;
            this.help = help;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.step = step;
            this.options = options;
            this.maxLength = maxLength;
        }
    }

    /** Entries are either a group name ({@link String}) or a {@link Row}. */
    public final List<Object> entries = new ArrayList<Object>();
    public final List<Row> rows = new ArrayList<Row>();
    public final VoidrunConfig draft;
    public final VoidrunConfig original;
    public int editingRow = -1;
    public String message;

    public SettingsModel(VoidrunConfig base) {
        this.original = base;
        this.draft = base.copy();
        this.message = base.loadWarning;
        buildRows();
    }

    private void buildRows() {
        group("Run");
        row("name", "Benchmark name", "Shown on the results screen and used in the file name.",
                Kind.TEXT, 0, 0, 0, null, 40);
        row("duration", "Duration", "Length of the measured part of the run, in seconds.",
                Kind.SLIDER, VoidrunConfig.DURATION_MIN, VoidrunConfig.DURATION_MAX,
                VoidrunConfig.DURATION_STEP, null, 0);
        row("warmup", "Warm-up", "Seconds of frames thrown away before measuring starts.",
                Kind.SLIDER, VoidrunConfig.WARMUP_MIN, VoidrunConfig.WARMUP_MAX, 1, null, 0);
        row("spawn", "Spawn height", "Height the player is locked at. The world stays empty.",
                Kind.SLIDER, VoidrunConfig.SPAWN_MIN, VoidrunConfig.SPAWN_MAX, 1, null, 0);
        row("yaw", "Camera yaw", "Fixed camera yaw during the run, in degrees.",
                Kind.SLIDER, VoidrunConfig.YAW_MIN, VoidrunConfig.YAW_MAX, 5, null, 0);
        row("pitch", "Camera pitch", "Fixed camera pitch during the run, in degrees.",
                Kind.SLIDER, VoidrunConfig.PITCH_MIN, VoidrunConfig.PITCH_MAX, 5, null, 0);

        group("Output");
        row("pattern", "File name pattern", "Tokens: {name}, {date} and {time}.",
                Kind.TEXT, 0, 0, 0, null, 60);
        row("folder", "Output folder", "Folder inside the game directory. Sub folders are fine.",
                Kind.TEXT, 0, 0, 0, null, 60);
        row("format", "Save format", "Which result files are written.",
                Kind.CYCLE, 0, 0, 0, VoidrunConfig.SAVE_FORMAT_LABELS, 0);
        row("autosave", "Auto save results", "Write the result files as soon as a run ends.",
                Kind.TOGGLE, 0, 0, 0, null, 0);
        row("autocopy", "Auto copy summary", "Copy the summary to the clipboard when a run ends.",
                Kind.TOGGLE, 0, 0, 0, null, 0);

        group("Overlay");
        row("overlay", "Show overlay", "Small time and FPS readout while a run is active.",
                Kind.TOGGLE, 0, 0, 0, null, 0);
        row("corner", "Overlay corner", "Corner of the screen used by the overlay.",
                Kind.CYCLE, 0, 0, 0, VoidrunConfig.CORNER_LABELS, 0);
        row("showtime", "Show time left", "Show the time left in the overlay.",
                Kind.TOGGLE, 0, 0, 0, null, 0);
        row("showfps", "Show current FPS", "Show the current frame rate in the overlay.",
                Kind.TOGGLE, 0, 0, 0, null, 0);

        group("Results screen");
        row("graph", "Show frame graph", "Draw the frame rate graph on the results screen.",
                Kind.TOGGLE, 0, 0, 0, null, 0);
        row("animations", "Animations", "Turn off to show the results instantly.",
                Kind.TOGGLE, 0, 0, 0, null, 0);

        group("Run control");
        row("confirm", "Confirm before cancelling", "Ask before Esc stops an active run.",
                Kind.TOGGLE, 0, 0, 0, null, 0);
    }

    private void group(String name) {
        entries.add(name);
    }

    private void row(String key, String label, String help, Kind kind, int min, int max, int step,
                     String[] options, int maxLength) {
        Row row = new Row(key, label, help, kind, min, max, step, options, maxLength);
        entries.add(row);
        rows.add(row);
    }

    // ------------------------------------------------------------------ values

    public String valueText(Row row) {
        if (row.kind == Kind.TEXT) {
            return textValue(row);
        }
        if (row.kind == Kind.TOGGLE) {
            return boolValue(row) ? "On" : "Off";
        }
        if (row.kind == Kind.CYCLE) {
            return cycleLabel(row);
        }
        if ("duration".equals(row.key)) {
            return draft.durationLabel();
        }
        if ("warmup".equals(row.key)) {
            return draft.warmupSeconds + " s";
        }
        if ("spawn".equals(row.key)) {
            return String.valueOf(draft.spawnHeight);
        }
        if ("yaw".equals(row.key)) {
            return draft.cameraYaw + " deg";
        }
        if ("pitch".equals(row.key)) {
            return draft.cameraPitch + " deg";
        }
        return "";
    }

    public float fraction(Row row) {
        int value = intValue(row);
        int span = row.max - row.min;
        return span <= 0 ? 0.0f : (value - row.min) / (float) span;
    }

    public void setFraction(Row row, float fraction) {
        float f = Ui.clamp01(fraction);
        int raw = row.min + Math.round(f * (row.max - row.min));
        if (row.step > 1) {
            raw = row.min + Math.round((raw - row.min) / (float) row.step) * row.step;
        }
        setInt(row, Math.max(row.min, Math.min(row.max, raw)));
    }

    public void cycle(Row row, int direction) {
        if (row.kind == Kind.TOGGLE) {
            setBool(row, !boolValue(row));
            return;
        }
        if (row.kind != Kind.CYCLE || row.options == null) {
            return;
        }
        int index = 0;
        String current = valueText(row);
        for (int i = 0; i < row.options.length; i++) {
            if (row.options[i].equals(current)) {
                index = i;
            }
        }
        index = (index + (direction >= 0 ? 1 : row.options.length - 1)) % row.options.length;
        if ("format".equals(row.key)) {
            draft.saveFormat = VoidrunConfig.SAVE_FORMATS[index];
        } else if ("corner".equals(row.key)) {
            draft.overlayCorner = VoidrunConfig.CORNERS[index];
        }
    }

    public String textValue(Row row) {
        return field(row);
    }

    public void setText(Row row, String value) {
        if ("name".equals(row.key)) {
            draft.benchmarkName = value;
        } else if ("pattern".equals(row.key)) {
            draft.fileNamePattern = value;
        } else if ("folder".equals(row.key)) {
            draft.outputFolder = value;
        }
    }

    public boolean boolValue(Row row) {
        if ("autosave".equals(row.key)) {
            return draft.autoSave;
        }
        if ("autocopy".equals(row.key)) {
            return draft.autoCopy;
        }
        if ("overlay".equals(row.key)) {
            return draft.showOverlay;
        }
        if ("showtime".equals(row.key)) {
            return draft.overlayShowTime;
        }
        if ("showfps".equals(row.key)) {
            return draft.overlayShowFps;
        }
        if ("graph".equals(row.key)) {
            return draft.showGraph;
        }
        if ("animations".equals(row.key)) {
            return draft.animations;
        }
        if ("confirm".equals(row.key)) {
            return draft.confirmCancel;
        }
        return false;
    }

    private void setBool(Row row, boolean value) {
        if ("autosave".equals(row.key)) {
            draft.autoSave = value;
        } else if ("autocopy".equals(row.key)) {
            draft.autoCopy = value;
        } else if ("overlay".equals(row.key)) {
            draft.showOverlay = value;
        } else if ("showtime".equals(row.key)) {
            draft.overlayShowTime = value;
        } else if ("showfps".equals(row.key)) {
            draft.overlayShowFps = value;
        } else if ("graph".equals(row.key)) {
            draft.showGraph = value;
        } else if ("animations".equals(row.key)) {
            draft.animations = value;
        } else if ("confirm".equals(row.key)) {
            draft.confirmCancel = value;
        }
    }

    private int intValue(Row row) {
        if ("duration".equals(row.key)) {
            return draft.durationSeconds;
        }
        if ("warmup".equals(row.key)) {
            return draft.warmupSeconds;
        }
        if ("spawn".equals(row.key)) {
            return draft.spawnHeight;
        }
        if ("yaw".equals(row.key)) {
            return draft.cameraYaw;
        }
        if ("pitch".equals(row.key)) {
            return draft.cameraPitch;
        }
        return row.min;
    }

    private void setInt(Row row, int value) {
        if ("duration".equals(row.key)) {
            draft.durationSeconds = value;
        } else if ("warmup".equals(row.key)) {
            draft.warmupSeconds = value;
        } else if ("spawn".equals(row.key)) {
            draft.spawnHeight = value;
        } else if ("yaw".equals(row.key)) {
            draft.cameraYaw = value;
        } else if ("pitch".equals(row.key)) {
            draft.cameraPitch = value;
        }
    }

    private String field(Row row) {
        if ("name".equals(row.key)) {
            return draft.benchmarkName;
        }
        if ("pattern".equals(row.key)) {
            return draft.fileNamePattern;
        }
        if ("folder".equals(row.key)) {
            return draft.outputFolder;
        }
        return "";
    }

    private String cycleLabel(Row row) {
        if ("format".equals(row.key)) {
            return draft.formatLabel();
        }
        if ("corner".equals(row.key)) {
            return draft.cornerLabel();
        }
        return "";
    }

    // ---------------------------------------------------------------- editing

    public void beginEdit(Row row) {
        if (row == null || row.kind != Kind.TEXT) {
            editingRow = -1;
            return;
        }
        editingRow = rows.indexOf(row);
    }

    public void endEdit() {
        editingRow = -1;
    }

    public Row editing() {
        return editingRow >= 0 && editingRow < rows.size() ? rows.get(editingRow) : null;
    }

    public void typeChar(char c) {
        Row row = editing();
        if (row == null) {
            return;
        }
        String value = field(row);
        if (value.length() >= row.maxLength) {
            return;
        }
        boolean allowed = "folder".equals(row.key)
                ? VoidrunConfig.allowedFolderChar(c) : VoidrunConfig.allowedNameChar(c);
        if ("pattern".equals(row.key) && (c == '{' || c == '}')) {
            allowed = true;
        }
        if (allowed) {
            setText(row, value + c);
        }
    }

    public void backspace() {
        Row row = editing();
        if (row == null) {
            return;
        }
        String value = field(row);
        if (value.length() > 0) {
            setText(row, value.substring(0, value.length() - 1));
        }
    }

    public void resetToDefaults() {
        VoidrunConfig defaults = new VoidrunConfig();
        draft.benchmarkName = defaults.benchmarkName;
        draft.durationSeconds = defaults.durationSeconds;
        draft.warmupSeconds = defaults.warmupSeconds;
        draft.fileNamePattern = defaults.fileNamePattern;
        draft.outputFolder = defaults.outputFolder;
        draft.saveFormat = defaults.saveFormat;
        draft.autoSave = defaults.autoSave;
        draft.autoCopy = defaults.autoCopy;
        draft.spawnHeight = defaults.spawnHeight;
        draft.cameraYaw = defaults.cameraYaw;
        draft.cameraPitch = defaults.cameraPitch;
        draft.showOverlay = defaults.showOverlay;
        draft.overlayCorner = defaults.overlayCorner;
        draft.overlayShowTime = defaults.overlayShowTime;
        draft.overlayShowFps = defaults.overlayShowFps;
        draft.showGraph = defaults.showGraph;
        draft.confirmCancel = defaults.confirmCancel;
        draft.animations = defaults.animations;
        editingRow = -1;
        message = "Defaults restored. Click Done to keep them.";
    }

    public boolean dirty() {
        return !draft.sameAs(original);
    }

    /** Validates and saves. Returns null on success, or a message to show. */
    public String apply(File gameDir) {
        String error = draft.validationError();
        if (error != null) {
            return error;
        }
        draft.sanitize();
        try {
            draft.save(gameDir);
        } catch (Throwable t) {
            return "Could not save the config: " + t.getMessage();
        }
        return null;
    }
}
