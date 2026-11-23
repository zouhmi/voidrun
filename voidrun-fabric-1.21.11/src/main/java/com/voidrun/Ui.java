package com.voidrun;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Colour maths, easing, formatting and the few shared widget shapes. */
public final class Ui {
    private Ui() {
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Multiplies an ARGB colour's alpha by factor (0..1). */
    public static int fade(int color, float factor) {
        if (factor <= 0.0f) {
            return color & 0xFFFFFF;
        }
        if (factor >= 1.0f) {
            return color;
        }
        int alpha = (int) (((color >>> 24) & 0xFF) * factor);
        return withAlpha(color, alpha);
    }

    public static int lerp(int from, int to, float t) {
        if (t <= 0.0f) {
            return from;
        }
        if (t >= 1.0f) {
            return to;
        }
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int) (((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public static float easeOut(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    /** Eased 0..1 progress of an animation that starts at delayMs and lasts durMs. */
    public static float anim(long elapsedMs, long delayMs, long durMs) {
        if (durMs <= 0L) {
            return elapsedMs >= delayMs ? 1.0f : 0.0f;
        }
        return easeOut((elapsedMs - delayMs) / (float) durMs);
    }

    // --------------------------------------------------------------- shapes

    public static void panel(Painter p, float x, float y, float w, float h) {
        p.rect(x, y, w, h, UiSpec.PANEL);
        p.rect(x, y, w, 1.0f, UiSpec.BORDER);
        p.rect(x, y + h - 1.0f, w, 1.0f, UiSpec.BORDER);
        p.rect(x, y + 1.0f, 1.0f, h - 2.0f, UiSpec.BORDER);
        p.rect(x + w - 1.0f, y + 1.0f, 1.0f, h - 2.0f, UiSpec.BORDER);
    }

    public static void rule(Painter p, float x, float y, float w) {
        p.rect(x, y, w, 1.0f, UiSpec.RULE);
    }

    public static void button(Painter p, float x, float y, float w, float h, String label,
                              boolean hover, boolean pressed, boolean enabled) {
        int fill = !enabled ? UiSpec.BTN_DOWN : (pressed ? UiSpec.BTN_DOWN
                : (hover ? UiSpec.BTN_HOVER : UiSpec.BTN_FILL));
        int light = pressed ? UiSpec.EDGE_SHADOW : UiSpec.EDGE_LIGHT;
        int shadow = pressed ? UiSpec.EDGE_LIGHT : UiSpec.EDGE_SHADOW;
        p.rect(x, y, w, h, UiSpec.EDGE_DARK);
        p.rect(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, fill);
        p.rect(x + 1.0f, y + 1.0f, w - 2.0f, 1.0f, light);
        p.rect(x + 1.0f, y + 1.0f, 1.0f, h - 2.0f, light);
        p.rect(x + 1.0f, y + h - 2.0f, w - 2.0f, 1.0f, shadow);
        p.rect(x + w - 2.0f, y + 1.0f, 1.0f, h - 2.0f, shadow);
        int color = enabled ? (hover ? UiSpec.TEXT : UiSpec.TEXT2) : UiSpec.MUTED;
        p.text(label, x + (w - p.textWidth(label)) / 2.0f, y + (h - 8.0f) / 2.0f, color, 1.0f);
    }

    /** Text field box, text clipped to the box, caret when focused. */
    public static void field(Painter p, float x, float y, float w, float h, String value,
                             boolean focused, long nowMs) {
        p.rect(x, y, w, h, UiSpec.EDGE_DARK);
        p.rect(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, UiSpec.FIELD);
        String shown = ellipsize(p, value == null ? "" : value, w - 8.0f);
        p.text(shown, x + 4.0f, y + (h - 8.0f) / 2.0f, UiSpec.TEXT, 1.0f);
        if (focused && ((nowMs / 500L) % 2L == 0L)) {
            float caretX = x + 4.0f + p.textWidth(shown) + 1.0f;
            p.rect(caretX, y + 3.0f, 1.0f, h - 6.0f, UiSpec.TEXT);
        }
    }

    public static void slider(Painter p, float x, float y, float w, float fraction, boolean focused) {
        float track = 4.0f;
        p.rect(x, y + 5.0f, w, track, UiSpec.BORDER);
        p.rect(x, y + 5.0f, Math.max(0.0f, w * clamp01(fraction)), track,
                focused ? UiSpec.TEXT : UiSpec.TEXT2);
        float knobX = x + w * clamp01(fraction) - 4.0f;
        if (knobX < x - 1.0f) {
            knobX = x - 1.0f;
        }
        if (knobX > x + w - 3.0f) {
            knobX = x + w - 3.0f;
        }
        p.rect(knobX, y + 1.0f, 7.0f, 12.0f, UiSpec.EDGE_DARK);
        p.rect(knobX + 1.0f, y + 2.0f, 5.0f, 10.0f, focused ? UiSpec.TEXT : UiSpec.TEXT2);
    }

    // ------------------------------------------------------------- formatting

    public static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", Double.valueOf(value));
    }

    /** Whole seconds as m:ss. */
    public static String clock(double seconds) {
        int total = (int) Math.ceil(Math.max(0.0, seconds));
        return String.format(Locale.ROOT, "%d:%02d", Integer.valueOf(total / 60),
                Integer.valueOf(total % 60));
    }

    public static String dateTime(long epochMs) {
        SimpleDateFormat f = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.ROOT);
        return f.format(new Date(epochMs));
    }

    /** Shortens text with "..." so it fits maxWidth virtual units. */
    public static String ellipsize(Painter p, String s, float maxWidth) {
        if (s == null) {
            return "";
        }
        if (p.textWidth(s) <= maxWidth) {
            return s;
        }
        String dots = "...";
        float dotsWidth = p.textWidth(dots);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (p.textWidth(sb.toString() + s.charAt(i)) + dotsWidth > maxWidth) {
                break;
            }
            sb.append(s.charAt(i));
        }
        return sb.append(dots).toString();
    }
}
