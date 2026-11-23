package com.voidrun;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** HUD readout shown during warm-up and the run. Layout lives in the shared OverlayView. */
public final class RunOverlay {
    private static long fadeStartMs = -1L;

    private RunOverlay() {
    }

    public static void reset() {
        fadeStartMs = -1L;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        RunManager rm = RunManager.INSTANCE;
        RunState state = rm.state();
        if (state != RunState.WARMUP && state != RunState.RUNNING) {
            fadeStartMs = -1L;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return;
        }
        VoidrunConfig cfg = rm.config();
        if (!cfg.showOverlay) {
            return;
        }

        long now = System.currentTimeMillis();
        if (fadeStartMs < 0L) {
            fadeStartMs = now;
        }
        float fade = Ui.anim(now - fadeStartMs, 0L, UiSpec.AN_CONTENT_MS);
        RenderUtil.begin(graphics, mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        OverlayView.render(RenderUtil.PAINTER, cfg, state == RunState.WARMUP,
                rm.phaseRemainingSec(), rm.liveFps(), fade);
        RenderUtil.end();
    }
}
