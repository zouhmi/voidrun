package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/** HUD readout shown during warm-up and the run. Layout lives in the shared OverlayView. */
public final class RunOverlay {
    private static long fadeStartMs = -1L;

    private RunOverlay() {
    }

    public static void reset() {
        fadeStartMs = -1L;
    }

    public static void render(Object ignored) {
        RunManager rm = RunManager.INSTANCE;
        RunState state = rm.state();
        if (state != RunState.WARMUP && state != RunState.RUNNING) {
            fadeStartMs = -1L;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null) {
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
        ScaledResolution resolution = new ScaledResolution(mc);
        RenderUtil.begin(resolution.getScaledWidth(), resolution.getScaledHeight());
        OverlayView.render(RenderUtil.PAINTER, cfg, state == RunState.WARMUP,
                rm.phaseRemainingSec(), rm.liveFps(), fade);
        RenderUtil.end();
    }
}
