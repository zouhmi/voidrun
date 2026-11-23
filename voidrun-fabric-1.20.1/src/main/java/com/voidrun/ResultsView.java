package com.voidrun;

import java.util.List;
import java.util.Locale;

/**
 * Draws the results screen on the virtual canvas and turns clicks into actions. The screen
 * itself only owns the platform window, clipboard and folder opening.
 */
public final class ResultsView {
    public static final int ACT_NONE = -1;
    public static final int ACT_COPY = 0;
    public static final int ACT_FOLDER = 1;
    public static final int ACT_SETTINGS = 2;
    public static final int ACT_AGAIN = 3;

    private static final int BTN_COPY = 0;
    private static final int BTN_FOLDER = 1;
    private static final int BTN_SETTINGS = 2;
    private static final int BTN_AGAIN = 3;
    private static final int BTN_DONE = 4;

    private final ResultData data;
    private final String note;
    private final boolean noteError;
    private final VoidrunConfig cfg;
    private final long openedMs = System.currentTimeMillis();

    private long closingMs = -1L;
    private int hovered = -1;
    private int pressed = -1;
    private String toast;
    private long toastUntilMs;
    private long copiedUntilMs;
    private float alpha = 1.0f;

    public ResultsView(ResultData data, String note, boolean noteError, VoidrunConfig cfg) {
        this.data = data;
        this.note = note == null ? "" : note;
        this.noteError = noteError;
        this.cfg = cfg;
    }

    public boolean isClosing() {
        return closingMs >= 0L;
    }

    /** 1 while open, falling to 0 during the close fade. Used for the screen backdrop. */
    public float fade() {
        if (closingMs < 0L) {
            return 1.0f;
        }
        return 1.0f - Ui.clamp01(
                (System.currentTimeMillis() - closingMs) / (float) UiSpec.AN_CLOSE_MS);
    }

    /** True once the close fade has finished, so the caller can dismiss the run. */
    public boolean closeFinished() {
        return closingMs >= 0L
                && System.currentTimeMillis() - closingMs >= UiSpec.AN_CLOSE_MS;
    }

    public void close() {
        if (closingMs < 0L) {
            closingMs = System.currentTimeMillis();
        }
    }

    public void toast(String message) {
        toast = message;
        toastUntilMs = System.currentTimeMillis() + UiSpec.AN_TOAST_MS;
    }

    public void markCopied() {
        copiedUntilMs = System.currentTimeMillis() + UiSpec.AN_COPIED_MS;
    }

    // ------------------------------------------------------------------ draw

    public void render(Painter p, float mouseX, float mouseY) {
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long t = now - openedMs;
        float closeT = closingMs < 0L ? 0.0f
                : Ui.clamp01((now - closingMs) / (float) UiSpec.AN_CLOSE_MS);
        float master = 1.0f - closeT;
        float intro = cfg.animations ? Ui.anim(t, 0L, UiSpec.AN_CONTENT_MS) : 1.0f;
        alpha = master * intro;

        drawHeader(p, now);
        drawMainNumber(p, t);
        drawTable(p, t);
        if (cfg.showGraph) {
            drawGraph(p, t, mouseX, mouseY);
        }
        drawWarnings(p);
        drawNote(p);
        drawButtons(p, now, mouseX, mouseY);
        drawToast(p, now);
    }

    private int color(int argb) {
        return Ui.fade(argb, alpha);
    }

    private void drawHeader(Painter p, long now) {
        p.text(UiSpec.TITLE, UiSpec.MARGIN, UiSpec.TITLE_Y, color(UiSpec.TEXT), 1.0f);
        String date = Ui.dateTime(data.timestampEpochMs > 0L ? data.timestampEpochMs : now);
        p.text(date, UiSpec.CONTENT_RIGHT - p.textWidth(date), UiSpec.TITLE_Y, color(UiSpec.MUTED),
                1.0f);

        StringBuilder meta = new StringBuilder();
        meta.append(data.benchmarkName).append("  |  ").append(data.system.width).append('x')
                .append(data.system.height).append("  |  render distance ")
                .append(data.system.renderDistance);
        String renderer = data.system.gpuRenderer;
        if (renderer != null && renderer.length() > 0 && !"unknown".equals(renderer)) {
            meta.append("  |  ").append(renderer);
        }
        p.text(Ui.ellipsize(p, meta.toString(), UiSpec.CONTENT_W), UiSpec.MARGIN, UiSpec.META_Y,
                color(UiSpec.MUTED), 1.0f);
        p.rect(UiSpec.MARGIN, UiSpec.RULE_Y, UiSpec.CONTENT_W, 1.0f, color(UiSpec.RULE));
    }

    private void drawMainNumber(Painter p, long t) {
        double countUp = cfg.animations
                ? Ui.anim(t, UiSpec.AN_NUMBER_DELAY_MS, UiSpec.AN_NUMBER_MS) : 1.0;
        p.text("Average FPS", UiSpec.MARGIN, UiSpec.MAIN_LABEL_Y, color(UiSpec.MUTED), 1.0f);

        String value = String.format(Locale.ROOT, "%.0f", Double.valueOf(data.avgFps * countUp));
        p.text(value, UiSpec.MARGIN, UiSpec.MAIN_NUMBER_Y, color(data.tierColor()), 3.0f);
        float valueWidth = p.textWidth(value) * 3.0f;
        p.text("fps", UiSpec.MARGIN + valueWidth + 6.0f, UiSpec.MAIN_NUMBER_Y + 19.0f,
                color(UiSpec.MUTED), 1.0f);

        p.text(data.tierLabel(), UiSpec.MARGIN, UiSpec.TIER_Y, color(data.tierColor()), 1.0f);

        String info = data.durationLabel() + " run  |  " + data.frames + " frames  |  "
                + data.mcVersion + " " + data.loader;
        p.text(Ui.ellipsize(p, info, 260.0f), UiSpec.MARGIN, UiSpec.RUN_INFO_Y, color(UiSpec.MUTED),
                1.0f);
    }

    private void drawTable(Painter p, long t) {
        double countUp = cfg.animations
                ? Ui.anim(t, UiSpec.AN_NUMBER_DELAY_MS, UiSpec.AN_NUMBER_MS) : 1.0;
        String[] leftLabels = {"1% low", "0.1% low", "Minimum FPS", "Maximum FPS", "Frame time"};
        String[] leftValues = {
                Ui.fmt(data.p1LowFps * countUp, 1),
                Ui.fmt(data.p01LowFps * countUp, 1),
                Ui.fmt(data.minFps * countUp, 1),
                Ui.fmt(data.maxFps * countUp, 1),
                Ui.fmt(data.avgFrameMs * countUp, 2) + " ms"};
        String[] rightLabels = {"CPU load", "Memory", "Std dev", "Frames", "Duration"};
        String[] rightValues = {
                Ui.fmt(data.avgCpuPercent * countUp, 1) + " %",
                Ui.fmt(data.avgRamMb * countUp, 0) + " MB",
                Ui.fmt(data.stddevFrameMs * countUp, 2) + " ms",
                String.valueOf((int) Math.round(data.frames * countUp)),
                data.durationLabel()};

        for (int i = 0; i < UiSpec.TABLE_ROWS; i++) {
            float y = UiSpec.TABLE_Y + i * UiSpec.TABLE_ROW;
            tableRow(p, UiSpec.TABLE_LEFT_X, UiSpec.TABLE_LEFT_VALUE_X, y, leftLabels[i],
                    leftValues[i]);
            tableRow(p, UiSpec.TABLE_RIGHT_X, UiSpec.TABLE_RIGHT_VALUE_X, y, rightLabels[i],
                    rightValues[i]);
        }
    }

    private void tableRow(Painter p, float labelX, float valueX, float y, String label,
                          String value) {
        p.text(label, labelX, y, color(UiSpec.MUTED), 1.0f);
        p.text(value, valueX - p.textWidth(value), y, color(UiSpec.TEXT), 1.0f);
        p.rect(labelX, y + 12.0f, valueX - labelX, 1.0f, color(UiSpec.RULE));
    }

    private void drawGraph(Painter p, long t, float mouseX, float mouseY) {
        Ui.panel(p, UiSpec.GRAPH_X, UiSpec.GRAPH_Y, UiSpec.GRAPH_W, UiSpec.GRAPH_H);
        p.text("Frame rate over time", UiSpec.GRAPH_PLOT_X, 132.0f, color(UiSpec.TEXT2), 1.0f);
        String avg = "average " + Ui.fmt(data.avgFps, 1) + " fps";
        p.text(avg, UiSpec.CONTENT_RIGHT - 8.0f - p.textWidth(avg), 132.0f, color(UiSpec.MUTED),
                1.0f);

        float px = UiSpec.GRAPH_PLOT_X;
        float py = UiSpec.GRAPH_PLOT_Y;
        float pw = UiSpec.GRAPH_PLOT_W;
        float ph = UiSpec.GRAPH_PLOT_H;

        for (int i = 0; i <= 4; i++) {
            float gy = py + ph * i / 4.0f;
            p.rect(px, gy, pw, 1.0f, color(i == 4 ? UiSpec.BORDER : UiSpec.RULE));
        }

        double[] times = StatsCalculator.downsample(data.frameTimesMs, UiSpec.GRAPH_MAX_POINTS);
        double yMax = Math.max(60.0, data.maxFps) * 1.05;
        if (yMax <= 0.0) {
            yMax = 60.0;
        }
        float reveal = cfg.animations
                ? Ui.anim(t, UiSpec.AN_GRAPH_DELAY_MS, UiSpec.AN_GRAPH_MS) : 1.0f;
        int columns = Math.max(1, (int) (pw * reveal));
        int n = times.length;

        if (n > 0) {
            int lineColor = color(UiSpec.TEXT2);
            int prevY = -1;
            for (int col = 0; col < columns; col++) {
                int idx = (int) Math.round((double) col / Math.max(1, pw - 1.0f) * (n - 1));
                idx = Math.max(0, Math.min(n - 1, idx));
                double ms = times[idx];
                double fps = ms > 0.0 ? 1000.0 / ms : 0.0;
                fps = Math.min(fps, yMax);
                float valueY = py + ph - (float) (fps / yMax * ph);
                if (prevY >= 0) {
                    float top = Math.min(prevY, valueY);
                    float height = Math.max(1.0f, Math.abs(valueY - prevY));
                    p.rect(px + col, top, 1.0f, height, lineColor);
                }
                p.rect(px + col, valueY, 1.0f, 1.0f, lineColor);
                prevY = (int) valueY;
            }
        }

        float avgY = py + ph - (float) (Math.min(data.avgFps, yMax) / yMax * ph);
        int dashColor = color(Ui.withAlpha(data.tierColor(), 170));
        for (float dx = 0.0f; dx < pw; dx += 8.0f) {
            p.rect(px + dx, avgY, 4.0f, 1.0f, dashColor);
        }

        // y labels at the right edge of the plot
        String top = Ui.fmt(yMax, 0);
        String mid = Ui.fmt(yMax / 2.0, 0);
        p.text(top, px + pw - p.textWidth(top) - 4.0f, py + 2.0f, color(UiSpec.MUTED), 1.0f);
        p.text(mid, px + pw - p.textWidth(mid) - 4.0f, py + ph / 2.0f + 2.0f,
                color(UiSpec.MUTED), 1.0f);
        p.text("0", px + pw - p.textWidth("0") - 4.0f, py + ph - 9.0f, color(UiSpec.MUTED), 1.0f);

        p.text("0:00", px, UiSpec.GRAPH_XLABEL_Y, color(UiSpec.MUTED), 1.0f);
        String half = Ui.clock(data.durationSec / 2.0);
        p.text(half, px + pw / 2.0f - p.textWidth(half) / 2.0f, UiSpec.GRAPH_XLABEL_Y,
                color(UiSpec.MUTED), 1.0f);
        String end = Ui.clock(data.durationSec);
        p.text(end, px + pw - p.textWidth(end), UiSpec.GRAPH_XLABEL_Y, color(UiSpec.MUTED), 1.0f);

        if (n > 0 && mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + ph) {
            p.rect(mouseX, py, 1.0f, ph, color(Ui.withAlpha(UiSpec.TEXT2, 120)));
            int idx = (int) Math.round((mouseX - px) / Math.max(1.0f, pw) * (n - 1));
            idx = Math.max(0, Math.min(n - 1, idx));
            double ms = times[idx];
            double fps = ms > 0.0 ? 1000.0 / ms : 0.0;
            String label = Ui.fmt(fps, 0) + " fps";
            float labelX = Math.min(mouseX + 4.0f, px + pw - p.textWidth(label));
            p.text(label, labelX, py + 2.0f, color(UiSpec.TEXT), 1.0f);
        }
    }

    private void drawWarnings(Painter p) {
        List<String> warnings = data.warnings();
        for (int i = 0; i < warnings.size() && i < 2; i++) {
            p.text(Ui.ellipsize(p, warnings.get(i), UiSpec.CONTENT_W), UiSpec.MARGIN,
                    UiSpec.WARN_Y + i * 11.0f, color(UiSpec.WARN), 1.0f);
        }
    }

    private void drawNote(Painter p) {
        String text = note;
        if (noteError) {
            p.text(Ui.ellipsize(p, text, UiSpec.CONTENT_W), UiSpec.MARGIN, UiSpec.NOTE_Y,
                    color(UiSpec.WARN), 1.0f);
            return;
        }
        p.text(Ui.ellipsize(p, text, UiSpec.CONTENT_W), UiSpec.MARGIN, UiSpec.NOTE_Y,
                color(UiSpec.MUTED), 1.0f);
    }

    private void drawToast(Painter p, long now) {
        if (toast == null || now >= toastUntilMs) {
            return;
        }
        p.text(toast, UiSpec.MARGIN, UiSpec.TOAST_Y, color(UiSpec.TEXT2), 1.0f);
    }

    // ---------------------------------------------------------------- buttons

    private void drawButtons(Painter p, long now, float mouseX, float mouseY) {
        int hover = hitTest(mouseX, mouseY);
        hovered = hover;
        boolean copied = now < copiedUntilMs;
        button(p, BTN_COPY, UiSpec.BTN_COPY_X, UiSpec.BTN_COPY_W,
                copied ? UiSpec.BTN_COPIED : UiSpec.BTN_COPY, hover == BTN_COPY);
        button(p, BTN_FOLDER, UiSpec.BTN_FOLDER_X, UiSpec.BTN_FOLDER_W, UiSpec.BTN_FOLDER,
                hover == BTN_FOLDER);
        button(p, BTN_SETTINGS, UiSpec.BTN_SETTINGS_X, UiSpec.BTN_SETTINGS_W, UiSpec.BTN_SETTINGS,
                hover == BTN_SETTINGS);
        button(p, BTN_AGAIN, UiSpec.BTN_AGAIN_X, UiSpec.BTN_AGAIN_W, UiSpec.BTN_AGAIN,
                hover == BTN_AGAIN);
        button(p, BTN_DONE, UiSpec.BTN_DONE_X, UiSpec.BTN_DONE_W, UiSpec.BTN_DONE,
                hover == BTN_DONE);
    }

    private void button(Painter p, int id, float x, float w, String label, boolean hover) {
        Ui.button(p, x, UiSpec.BTN_Y, w, UiSpec.BTN_H, label, hover, pressed == id, true);
    }

    private int hitTest(float x, float y) {
        if (y < UiSpec.BTN_Y || y > UiSpec.BTN_Y + UiSpec.BTN_H) {
            return -1;
        }
        if (x >= UiSpec.BTN_COPY_X && x <= UiSpec.BTN_COPY_X + UiSpec.BTN_COPY_W) {
            return BTN_COPY;
        }
        if (x >= UiSpec.BTN_FOLDER_X && x <= UiSpec.BTN_FOLDER_X + UiSpec.BTN_FOLDER_W) {
            return BTN_FOLDER;
        }
        if (x >= UiSpec.BTN_SETTINGS_X && x <= UiSpec.BTN_SETTINGS_X + UiSpec.BTN_SETTINGS_W) {
            return BTN_SETTINGS;
        }
        if (x >= UiSpec.BTN_AGAIN_X && x <= UiSpec.BTN_AGAIN_X + UiSpec.BTN_AGAIN_W) {
            return BTN_AGAIN;
        }
        if (x >= UiSpec.BTN_DONE_X && x <= UiSpec.BTN_DONE_X + UiSpec.BTN_DONE_W) {
            return BTN_DONE;
        }
        return -1;
    }

    /** Handles a left click in virtual coordinates and returns the action the screen must run. */
    public int click(float x, float y) {
        if (closingMs >= 0L) {
            return ACT_NONE;
        }
        int hit = hitTest(x, y);
        if (hit < 0) {
            return ACT_NONE;
        }
        pressed = hit;
        switch (hit) {
            case BTN_COPY:
                markCopied();
                toast(UiSpec.TOAST_COPIED);
                return ACT_COPY;
            case BTN_FOLDER:
                return ACT_FOLDER;
            case BTN_SETTINGS:
                return ACT_SETTINGS;
            case BTN_AGAIN:
                return ACT_AGAIN;
            default:
                close();
                return ACT_NONE;
        }
    }

    /** Press state is cleared on mouse release so buttons do not stay stuck down. */
    public void release() {
        pressed = -1;
    }
}
