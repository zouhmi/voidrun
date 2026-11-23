package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;

/** Drawing helpers for the 640x360 virtual canvas. Call begin() first, end() last. */
public final class RenderUtil {
    private static GuiGraphics graphics;
    private static float scale = 1.0f;
    private static float offX;
    private static float offY;
    private static boolean active;

    /** Shared views draw through this. */
    public static final Painter PAINTER = new Painter() {
        @Override
        public void rect(float x, float y, float w, float h, int argb) {
            RenderUtil.rect(x, y, w, h, argb);
        }

        @Override
        public void rectGradient(float x, float y, float w, float h, int top, int bottom) {
            RenderUtil.rectGradient(x, y, w, h, top, bottom);
        }

        @Override
        public void text(String s, float x, float y, int argb, float textScale) {
            RenderUtil.text(s, x, y, argb, textScale);
        }

        @Override
        public int textWidth(String s) {
            return RenderUtil.textWidth(s);
        }

        @Override
        public void scissor(float x, float y, float w, float h) {
            RenderUtil.scissor(x, y, w, h);
        }

        @Override
        public void endScissor() {
            RenderUtil.endScissor();
        }
    };

    private RenderUtil() {
    }

    public static void begin(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        graphics = guiGraphics;
        int w = Math.max(1, screenWidth);
        int h = Math.max(1, screenHeight);
        scale = Math.min(w / (float) UiSpec.VW, h / (float) UiSpec.VH);
        if (scale <= 0.0f) {
            scale = 1.0f;
        }
        offX = (w - UiSpec.VW * scale) / 2.0f;
        offY = (h - UiSpec.VH * scale) / 2.0f;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(offX, offY, 0.0f);
        pose.scale(scale, scale, 1.0f);
        active = true;
    }

    public static void end() {
        if (!active) {
            return;
        }
        graphics.pose().popPose();
        active = false;
    }

    public static float mouseX(double mouseX) {
        return (float) ((mouseX - offX) / scale);
    }

    public static float mouseY(double mouseY) {
        return (float) ((mouseY - offY) / scale);
    }

    public static void scissor(float x, float y, float w, float h) {
        graphics.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
    }

    public static void endScissor() {
        graphics.disableScissor();
    }

    public static void rect(float x, float y, float w, float h, int color) {
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        graphics.fill(x1, y1, Math.round(x + w), Math.round(y + h), color);
    }

    public static void rectGradient(float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        graphics.fillGradient(x1, y1, Math.round(x + w), Math.round(y + h), top, bottom);
    }

    public static int textWidth(String s) {
        return font().width(s);
    }

    public static void text(String s, float x, float y, int color, float textScale) {
        if (s == null || s.length() == 0) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0f);
        if (textScale != 1.0f) {
            pose.scale(textScale, textScale, 1.0f);
        }
        graphics.drawString(font(), s, 0, 0, color, false);
        pose.popPose();
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }
}
