package com.voidrun;

/**
 * Shared visual constants for both loaders. Everything is drawn on a virtual 640x360 canvas,
 * so the Forge 1.8.9 and Fabric 1.21.11 screens match as closely as their APIs allow.
 *
 * <p>Values live here only. See spec/DESIGN.md for the reasoning.
 */
public final class UiSpec {
    private UiSpec() {
    }

    public static final int VW = 640;
    public static final int VH = 360;

    // ------------------------------------------------------------------ palette

    public static final int BG = 0xFF101010;
    public static final int PANEL = 0xF01A1A1A;
    public static final int ROW = 0xFF232323;
    public static final int BORDER = 0xFF3C3C3C;
    public static final int RULE = 0xFF2A2A2A;
    public static final int EDGE_DARK = 0xFF0A0A0A;
    public static final int EDGE_LIGHT = 0xFF4F4F4F;
    public static final int EDGE_SHADOW = 0xFF181818;
    public static final int BTN_FILL = 0xFF2E2E2E;
    public static final int BTN_HOVER = 0xFF3A3A3A;
    public static final int BTN_DOWN = 0xFF262626;
    public static final int FIELD = 0xFF141414;
    public static final int TEXT = 0xFFE0E0E0;
    public static final int TEXT2 = 0xFFA8A8A8;
    public static final int MUTED = 0xFF7A7A7A;
    public static final int WARN = 0xFFC79A5B;

    public static final int TIER_EXCELLENT = 0xFF6FAF6F;
    public static final int TIER_GREAT = 0xFF8FBF6F;
    public static final int TIER_GOOD = 0xFFC2BE62;
    public static final int TIER_FAIR = 0xFFC79A5B;
    public static final int TIER_POOR = 0xFFC46A5F;

    // ------------------------------------------------------------------- strings

    public static final String TITLE = "Benchmark results";
    public static final String SETTINGS_TITLE = "Benchmark settings";
    public static final String BTN_COPY = "Copy results";
    public static final String BTN_COPIED = "Copied";
    public static final String BTN_FOLDER = "Open folder";
    public static final String BTN_SETTINGS = "Settings";
    public static final String BTN_AGAIN = "Run again";
    public static final String BTN_DONE = "Done";
    public static final String BTN_CANCEL = "Cancel";
    public static final String BTN_RESET = "Reset to defaults";
    public static final String TOAST_COPIED = "Copied to clipboard";
    public static final String TOAST_OPENED = "Opened the results folder";

    // ------------------------------------------------------------ results layout

    public static final int MARGIN = 16;
    public static final int CONTENT_RIGHT = 624;
    public static final int CONTENT_W = 608;

    public static final int TITLE_Y = 12;
    public static final int META_Y = 24;
    public static final int RULE_Y = 36;
    public static final int MAIN_LABEL_Y = 44;
    public static final int MAIN_NUMBER_Y = 54;
    public static final int TIER_Y = 88;
    public static final int RUN_INFO_Y = 102;

    public static final int TABLE_LEFT_X = 296;
    public static final int TABLE_LEFT_VALUE_X = 448;
    public static final int TABLE_RIGHT_X = 464;
    public static final int TABLE_RIGHT_VALUE_X = 624;
    public static final int TABLE_Y = 44;
    public static final int TABLE_ROW = 16;
    public static final int TABLE_ROWS = 5;

    public static final int GRAPH_X = 16;
    public static final int GRAPH_Y = 124;
    public static final int GRAPH_W = 608;
    public static final int GRAPH_H = 124;
    public static final int GRAPH_PLOT_X = 24;
    public static final int GRAPH_PLOT_Y = 148;
    public static final int GRAPH_PLOT_W = 592;
    public static final int GRAPH_PLOT_H = 76;
    public static final int GRAPH_XLABEL_Y = 228;

    public static final int WARN_Y = 250;
    public static final int NOTE_Y = 272;
    public static final int BTN_Y = 288;
    public static final int BTN_H = 20;
    public static final int TOAST_Y = 328;

    public static final int BTN_COPY_X = 16;
    public static final int BTN_COPY_W = 104;
    public static final int BTN_FOLDER_X = 128;
    public static final int BTN_FOLDER_W = 104;
    public static final int BTN_SETTINGS_X = 240;
    public static final int BTN_SETTINGS_W = 88;
    public static final int BTN_AGAIN_X = 336;
    public static final int BTN_AGAIN_W = 104;
    public static final int BTN_DONE_X = 528;
    public static final int BTN_DONE_W = 96;

    // ----------------------------------------------------------- settings layout

    public static final int SET_VIEW_X = 16;
    public static final int SET_VIEW_Y = 32;
    public static final int SET_VIEW_W = 600;
    public static final int SET_VIEW_H = 252;
    public static final int SET_ROW_H = 24;
    public static final int SET_GROUP_H = 20;
    public static final int SET_LABEL_X = 32;
    public static final int SET_CONTROL_X = 352;
    public static final int SET_CONTROL_W = 248;
    public static final int SET_SLIDER_X = 352;
    public static final int SET_SLIDER_W = 168;
    public static final int SET_HELP_Y = 292;
    public static final int SET_BTN_Y = 306;
    public static final int SET_BTN_H = 20;
    public static final int SET_RESET_X = 16;
    public static final int SET_RESET_W = 132;
    public static final int SET_CANCEL_X = 492;
    public static final int SET_CANCEL_W = 60;
    public static final int SET_DONE_X = 560;
    public static final int SET_DONE_W = 64;

    // ------------------------------------------------------------- overlay layout

    public static final int OV_MARGIN = 8;
    public static final int OV_PAD = 8;
    public static final int OV_LINE_H = 9;
    public static final int OV_LINE_GAP = 3;

    // ---------------------------------------------------------------- animations

    public static final long AN_CONTENT_MS = 180L;
    public static final long AN_NUMBER_DELAY_MS = 100L;
    public static final long AN_NUMBER_MS = 600L;
    public static final long AN_GRAPH_DELAY_MS = 180L;
    public static final long AN_GRAPH_MS = 500L;
    public static final long AN_TOAST_MS = 1800L;
    public static final long AN_COPIED_MS = 1400L;
    public static final long AN_CLOSE_MS = 140L;

    // ----------------------------------------------------------------------- run

    public static final double LOADING_SECONDS = 1.0;
    public static final long GUARD_EXTRA_SECONDS = 30L;
    public static final int FPS_BUCKETS = 30;
    public static final int GRAPH_MAX_POINTS = 480;
    public static final String OVERLAY_WARMUP = "warm-up";
    public static final String OVERLAY_LEFT = " left";
}
