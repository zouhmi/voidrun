package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Small confirmation dialog shown when Esc would stop a run. */
public final class AbortConfirmScreen extends GuiScreen {
    private static final float PANEL_X = 160.0f;
    private static final float PANEL_Y = 126.0f;
    private static final float PANEL_W = 320.0f;
    private static final float PANEL_H = 104.0f;
    private static final float STOP_X = 176.0f;
    private static final float KEEP_X = 364.0f;
    private static final float BTN_Y = 188.0f;
    private static final float BTN_W = 100.0f;
    private static final float BTN_H = 20.0f;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, Ui.withAlpha(UiSpec.BG, 140));
        RenderUtil.begin(width, height);
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);
        if (hit(vx, vy, STOP_X)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            RunManager.INSTANCE.abort();
        } else if (hit(vx, vy, KEEP_X)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }
}
