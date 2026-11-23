package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Drawing helpers for the 640x360 virtual canvas. Call begin() first, end() last. */
public final class RenderUtil {
    private static float scale = 1.0f;
    private static float offX;
    private static float offY;
    private static int screenH = 1;
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

    public static void begin(int screenWidth, int screenHeight) {
        int w = Math.max(1, screenWidth);
        int h = Math.max(1, screenHeight);
        screenH = h;
        scale = Math.min(w / (float) UiSpec.VW, h / (float) UiSpec.VH);
        if (scale <= 0.0f) {
            scale = 1.0f;
        }
        offX = (w - UiSpec.VW * scale) / 2.0f;
        offY = (h - UiSpec.VH * scale) / 2.0f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(offX, offY, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        active = true;
    }

    public static void end() {
        if (!active) {
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
        active = false;
    }

    public static float mouseX(int mouseX) {
        return (mouseX - offX) / scale;
    }

    public static float mouseY(int mouseY) {
        return (mouseY - offY) / scale;
    }

    public static void scissor(float x, float y, float w, float h) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int sx = (int) (offX + x * scale);
        int sy = (int) Math.floor(screenH - (offY + (y + h) * scale));
        int sw = (int) Math.ceil(w * scale);
        int sh = (int) Math.ceil(h * scale);
        GL11.glScissor(sx, sy, Math.max(0, sw), Math.max(0, sh));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void rect(float x, float y, float w, float h, int color) {
        rectGradient(x, y, w, h, color, color);
    }

    public static void rectGradient(float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float a1 = ((top >>> 24) & 0xFF) / 255.0f;
        float r1 = ((top >>> 16) & 0xFF) / 255.0f;
        float g1 = ((top >>> 8) & 0xFF) / 255.0f;
        float b1 = (top & 0xFF) / 255.0f;
        float a2 = ((bottom >>> 24) & 0xFF) / 255.0f;
        float r2 = ((bottom >>> 16) & 0xFF) / 255.0f;
        float g2 = ((bottom >>> 8) & 0xFF) / 255.0f;
        float b2 = (bottom & 0xFF) / 255.0f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y + h, 0.0D).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + w, y + h, 0.0D).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + w, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        tess.draw();
    }

    public static int textWidth(String s) {
        return font().getStringWidth(s);
    }

    public static void text(String s, float x, float y, int color, float textScale) {
        if (s == null || s.length() == 0) {
            return;
        }
        FontRenderer fr = font();
        int rgb = color & 0xFFFFFF;
        int alpha = (color >>> 24) & 0xFF;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0f);
        GlStateManager.scale(textScale, textScale, 1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        fr.drawString(s, 0, 0, rgb);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableTexture2D();
        GlStateManager.popMatrix();
    }

    private static FontRenderer font() {
        return Minecraft.getMinecraft().fontRendererObj;
    }
}
