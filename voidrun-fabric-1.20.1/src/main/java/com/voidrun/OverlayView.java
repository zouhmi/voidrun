package com.voidrun;

/** Small two line readout drawn while a run is active. */
public final class OverlayView {
    private OverlayView() {
    }

    public static void render(Painter p, VoidrunConfig cfg, boolean warmup, double secondsLeft,
                              double fps, float fade) {
        if (!cfg.showOverlay || (!cfg.overlayShowTime && !cfg.overlayShowFps)) {
            return;
        }
        String line1 = warmup ? UiSpec.OVERLAY_WARMUP
                : (cfg.overlayShowTime ? Ui.clock(secondsLeft) + UiSpec.OVERLAY_LEFT : "");
        String line2 = cfg.overlayShowFps ? Ui.fmt(fps, 0) + " fps" : "";

        float width = Math.max(p.textWidth(line1), p.textWidth(line2)) + UiSpec.OV_PAD * 2.0f;
        float height = UiSpec.OV_PAD * 2.0f + UiSpec.OV_LINE_H;
        if (line1.length() > 0 && line2.length() > 0) {
            height += UiSpec.OV_LINE_GAP + UiSpec.OV_LINE_H;
        }

        float x;
        if ("top_right".equals(cfg.overlayCorner) || "bottom_right".equals(cfg.overlayCorner)) {
            x = UiSpec.VW - UiSpec.OV_MARGIN - width;
        } else {
            x = UiSpec.OV_MARGIN;
        }
        float y;
        if ("bottom_left".equals(cfg.overlayCorner) || "bottom_right".equals(cfg.overlayCorner)) {
            y = UiSpec.VH - UiSpec.OV_MARGIN - height;
        } else {
            y = UiSpec.OV_MARGIN;
        }

        p.rect(x, y, width, height, Ui.fade(Ui.withAlpha(UiSpec.PANEL, 230), fade));
        p.rect(x, y, width, 1.0f, Ui.fade(UiSpec.BORDER, fade));
        p.rect(x, y + height - 1.0f, width, 1.0f, Ui.fade(UiSpec.BORDER, fade));
        p.rect(x, y, 1.0f, height, Ui.fade(UiSpec.BORDER, fade));
        p.rect(x + width - 1.0f, y, 1.0f, height, Ui.fade(UiSpec.BORDER, fade));

        float textY = y + UiSpec.OV_PAD;
        if (line1.length() > 0) {
            p.text(line1, x + UiSpec.OV_PAD, textY, Ui.fade(UiSpec.TEXT, fade), 1.0f);
            textY += UiSpec.OV_LINE_H + UiSpec.OV_LINE_GAP;
        }
        if (line2.length() > 0) {
            p.text(line2, x + UiSpec.OV_PAD, textY, Ui.fade(UiSpec.TEXT2, fade), 1.0f);
        }
    }
}
