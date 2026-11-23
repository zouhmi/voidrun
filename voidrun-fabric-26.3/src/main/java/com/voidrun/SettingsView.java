package com.voidrun;

/** Draws the settings screen and answers hit tests for the platform screens. */
public final class SettingsView {
    public static final int BTN_RESET = 0;
    public static final int BTN_CANCEL = 1;
    public static final int BTN_DONE = 2;

    private static final String DEFAULT_HINT = "F8 opens this screen at any time.";

    private SettingsView() {
    }

    public static int contentHeight(SettingsModel model) {
        int height = 0;
        for (int i = 0; i < model.entries.size(); i++) {
            height += model.entries.get(i) instanceof String ? UiSpec.SET_GROUP_H
                    : UiSpec.SET_ROW_H;
        }
        return height;
    }

    public static int maxScroll(SettingsModel model) {
        return Math.max(0, contentHeight(model) - UiSpec.SET_VIEW_H);
    }

    public static int clampScroll(SettingsModel model, int scroll) {
        return Math.max(0, Math.min(maxScroll(model), scroll));
    }

    /** Top edge of an entry in viewport space, before the scroll offset is applied. */
    private static float entryOffset(SettingsModel model, int index) {
        float offset = 0.0f;
        for (int i = 0; i < index; i++) {
            offset += model.entries.get(i) instanceof String ? UiSpec.SET_GROUP_H
                    : UiSpec.SET_ROW_H;
        }
        return offset;
    }

    public static SettingsModel.Row rowAt(SettingsModel model, int scroll, float x, float y) {
        if (x < UiSpec.SET_VIEW_X || x > UiSpec.SET_VIEW_X + UiSpec.SET_VIEW_W
                || y < UiSpec.SET_VIEW_Y || y > UiSpec.SET_VIEW_Y + UiSpec.SET_VIEW_H) {
            return null;
        }
        for (int i = 0; i < model.entries.size(); i++) {
            float top = UiSpec.SET_VIEW_Y + entryOffset(model, i) - scroll;
            float height = model.entries.get(i) instanceof String ? UiSpec.SET_GROUP_H
                    : UiSpec.SET_ROW_H;
            if (y >= top && y < top + height) {
                Object entry = model.entries.get(i);
                return entry instanceof SettingsModel.Row ? (SettingsModel.Row) entry : null;
            }
        }
        return null;
    }

    public static boolean inControlArea(float x) {
        return x >= UiSpec.SET_CONTROL_X - 4.0f
                && x <= UiSpec.SET_CONTROL_X + UiSpec.SET_CONTROL_W;
    }

    public static boolean inSlider(float x) {
        return x >= UiSpec.SET_SLIDER_X - 6.0f && x <= UiSpec.SET_SLIDER_X + UiSpec.SET_SLIDER_W;
    }

    public static float sliderFraction(float x) {
        return Ui.clamp01((x - UiSpec.SET_SLIDER_X) / (float) UiSpec.SET_SLIDER_W);
    }

    public static int buttonAt(float x, float y) {
        if (y < UiSpec.SET_BTN_Y || y > UiSpec.SET_BTN_Y + UiSpec.SET_BTN_H) {
            return -1;
        }
        if (x >= UiSpec.SET_RESET_X && x <= UiSpec.SET_RESET_X + UiSpec.SET_RESET_W) {
            return BTN_RESET;
        }
        if (x >= UiSpec.SET_CANCEL_X && x <= UiSpec.SET_CANCEL_X + UiSpec.SET_CANCEL_W) {
            return BTN_CANCEL;
        }
        if (x >= UiSpec.SET_DONE_X && x <= UiSpec.SET_DONE_X + UiSpec.SET_DONE_W) {
            return BTN_DONE;
        }
        return -1;
    }

    // ------------------------------------------------------------------ draw

    public static void render(Painter p, SettingsModel model, int scroll, float mouseX,
                              float mouseY, int hoverButton, long nowMs) {
        p.text(UiSpec.SETTINGS_TITLE, UiSpec.MARGIN, UiSpec.TITLE_Y, UiSpec.TEXT, 1.0f);
        p.rect(UiSpec.MARGIN, 26.0f, UiSpec.CONTENT_W, 1.0f, UiSpec.RULE);

        SettingsModel.Row hovered = rowAt(model, scroll, mouseX, mouseY);
        p.scissor(UiSpec.SET_VIEW_X, UiSpec.SET_VIEW_Y, UiSpec.SET_VIEW_W, UiSpec.SET_VIEW_H);
        for (int i = 0; i < model.entries.size(); i++) {
            float y = UiSpec.SET_VIEW_Y + entryOffset(model, i) - scroll;
            Object entry = model.entries.get(i);
            if (entry instanceof String) {
                p.text((String) entry, UiSpec.SET_LABEL_X, y + 8.0f, UiSpec.TEXT2, 1.0f);
                p.rect(UiSpec.SET_LABEL_X, y + 18.0f, 400.0f, 1.0f, UiSpec.RULE);
                continue;
            }
            SettingsModel.Row row = (SettingsModel.Row) entry;
            boolean isHovered = row == hovered;
            if (isHovered) {
                p.rect(UiSpec.SET_VIEW_X + 1.0f, y, UiSpec.SET_VIEW_W - 2.0f,
                        (float) UiSpec.SET_ROW_H, UiSpec.ROW);
            }
            p.text(row.label, UiSpec.SET_LABEL_X, y + 8.0f, UiSpec.TEXT, 1.0f);
            drawControl(p, model, row, y, isHovered, nowMs);
        }
        p.endScissor();

        drawScrollbar(p, model, scroll);
        drawHelp(p, model, hovered);
        drawButtons(p, hoverButton);
    }

    private static void drawControl(Painter p, SettingsModel model, SettingsModel.Row row, float y,
                                    boolean hovered, long nowMs) {
        float controlY = y + 4.0f;
        if (row.kind == SettingsModel.Kind.TEXT) {
            boolean editing = model.editing() == row;
            Ui.field(p, UiSpec.SET_CONTROL_X, controlY, UiSpec.SET_CONTROL_W, 16.0f,
                    model.valueText(row), editing, nowMs);
            return;
        }
        if (row.kind == SettingsModel.Kind.SLIDER) {
            String value = model.valueText(row);
            p.text(value, UiSpec.SET_CONTROL_X + UiSpec.SET_CONTROL_W - p.textWidth(value),
                    y + 8.0f, UiSpec.TEXT, 1.0f);
            Ui.slider(p, UiSpec.SET_SLIDER_X, controlY, UiSpec.SET_SLIDER_W,
                    model.fraction(row), hovered);
            return;
        }
        Ui.button(p, UiSpec.SET_CONTROL_X, controlY, UiSpec.SET_CONTROL_W, 16.0f,
                model.valueText(row), hovered, false, true);
    }

    private static void drawScrollbar(Painter p, SettingsModel model, int scroll) {
        int max = maxScroll(model);
        if (max <= 0) {
            return;
        }
        float x = 620.0f;
        float height = UiSpec.SET_VIEW_H;
        p.rect(x, UiSpec.SET_VIEW_Y, 2.0f, height, UiSpec.EDGE_DARK);
        float thumb = Math.max(24.0f, height * UiSpec.SET_VIEW_H / (float) contentHeight(model));
        float thumbY = UiSpec.SET_VIEW_Y + (height - thumb) * (scroll / (float) max);
        p.rect(x, thumbY, 2.0f, thumb, UiSpec.BORDER);
    }

    private static void drawHelp(Painter p, SettingsModel model, SettingsModel.Row hovered) {
        String text;
        int color;
        if (hovered != null) {
            text = hovered.help;
            color = UiSpec.MUTED;
        } else if (model.message != null && model.message.length() > 0) {
            text = model.message;
            color = UiSpec.WARN;
        } else {
            text = DEFAULT_HINT;
            color = UiSpec.MUTED;
        }
        p.text(Ui.ellipsize(p, text, UiSpec.CONTENT_W), UiSpec.MARGIN, UiSpec.SET_HELP_Y, color,
                1.0f);
    }

    private static void drawButtons(Painter p, int hoverButton) {
        Ui.button(p, UiSpec.SET_RESET_X, UiSpec.SET_BTN_Y, UiSpec.SET_RESET_W, UiSpec.SET_BTN_H,
                UiSpec.BTN_RESET, hoverButton == BTN_RESET, false, true);
        Ui.button(p, UiSpec.SET_CANCEL_X, UiSpec.SET_BTN_Y, UiSpec.SET_CANCEL_W, UiSpec.SET_BTN_H,
                UiSpec.BTN_CANCEL, hoverButton == BTN_CANCEL, false, true);
        Ui.button(p, UiSpec.SET_DONE_X, UiSpec.SET_BTN_Y, UiSpec.SET_DONE_W, UiSpec.SET_BTN_H,
                UiSpec.BTN_DONE, hoverButton == BTN_DONE, false, true);
    }
}
