package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Small confirmation dialog shown when Esc would stop a run. */
public final class AbortConfirmScreen extends Screen {
    private static final float PANEL_X = 160.0f;
    private static final float PANEL_Y = 126.0f;
    private static final float PANEL_W = 320.0f;
    private static final float PANEL_H = 104.0f;
    private static final float STOP_X = 176.0f;
    private static final float KEEP_X = 364.0f;
    private static final float BTN_Y = 188.0f;
    private static final float BTN_W = 100.0f;
    private static final float BTN_H = 20.0f;

    public AbortConfirmScreen() {
        super(Component.literal("Stop the benchmark?"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The screen paints its own backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, Ui.withAlpha(UiSpec.BG, 140));
        RenderUtil.begin(graphics, width, height);
        Painter p = RenderUtil.PAINTER;
        Ui.panel(p, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        p.text("Stop the benchmark?", PANEL_X + 16.0f, PANEL_Y + 16.0f, UiSpec.TEXT, 1.0f);
        p.text("The run will not be saved.", PANEL_X + 16.0f, PANEL_Y + 32.0f, UiSpec.MUTED, 1.0f);

        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);
        Ui.button(p, STOP_X, BTN_Y, BTN_W, BTN_H, "Stop", hit(vx, vy, STOP_X), false, true);
        Ui.button(p, KEEP_X, BTN_Y, BTN_W, BTN_H, "Keep running", hit(vx, vy, KEEP_X), false, true);
        RenderUtil.end();
    }

    private static boolean hit(float vx, float vy, float x) {
        return vx >= x && vx <= x + BTN_W && vy >= BTN_Y && vy <= BTN_Y + BTN_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        float vx = RenderUtil.mouseX(event.x());
        float vy = RenderUtil.mouseY(event.y());
        if (hit(vx, vy, STOP_X)) {
            Minecraft.getInstance().gui.setScreen(null);
            RunManager.INSTANCE.abort();
        } else if (hit(vx, vy, KEEP_X)) {
            Minecraft.getInstance().gui.setScreen(null);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return false;
    }
}
