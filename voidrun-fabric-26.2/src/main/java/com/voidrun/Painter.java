package com.voidrun;

/** The drawing surface the shared views need. Each loader implements it in its RenderUtil. */
public interface Painter {
    void rect(float x, float y, float w, float h, int argb);

    void rectGradient(float x, float y, float w, float h, int top, int bottom);

    void text(String s, float x, float y, int argb, float scale);

    int textWidth(String s);

    void scissor(float x, float y, float w, float h);

    void endScissor();
}
