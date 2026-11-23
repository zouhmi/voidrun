package com.voidrun;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import org.lwjgl.glfw.GLFW;

/**
 * F7 starts a run, F8 opens the settings. Both work on the title screen.
 *
 * <p>Keys are read from the keybinding click queue, from screen key events and, as a last
 * resort, from polled key state. Launchers that replace the input backend answer polled state
 * with "not pressed" even while key callbacks fire normally.
 */
public final class KeybindHandler {
    private static KeyMapping startKey;
    private static KeyMapping settingsKey;
    private static boolean startDown;
    private static boolean settingsDown;
    private static boolean escDown;

    private KeybindHandler() {
    }

    public static void register() {
        startKey = new KeyMapping("key.voidrun.start", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, "Voidrun");
        settingsKey = new KeyMapping("key.voidrun.settings", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, "Voidrun");
        KeyBindingHelper.registerKeyBinding(startKey);
        KeyBindingHelper.registerKeyBinding(settingsKey);
    }

    public static void onClientTick(Minecraft mc) {
        while (startKey != null && startKey.consumeClick()) {
            if (tryStart()) {
                break;
            }
        }
        while (settingsKey != null && settingsKey.consumeClick()) {
            if (trySettings()) {
                break;
            }
        }

        boolean start = isDown(mc, startKey);
        if (start && !startDown) {
            tryStart();
        }
        startDown = start;

        boolean settings = isDown(mc, settingsKey);
        if (settings && !settingsDown) {
            trySettings();
        }
        settingsDown = settings;

        boolean esc = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE);
        if (esc && !escDown && RunManager.INSTANCE.isActive()) {
            if (mc.screen instanceof PauseScreen) {
                mc.setScreen(null);
            }
            RunManager.INSTANCE.requestAbort();
        }
        escDown = esc;
    }

    private static boolean isDown(Minecraft mc, KeyMapping mapping) {
        return mapping != null && mapping.isDown();
    }

    private static boolean tryStart() {
        if (RunManager.INSTANCE.state() != RunState.IDLE) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean fromTitle = mc.player == null || mc.level == null;
        RunManager.INSTANCE.startRun(fromTitle);
        return true;
    }

    private static boolean trySettings() {
        if (RunManager.INSTANCE.state() != RunState.IDLE) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SettingsScreen) {
            return false;
        }
        mc.setScreen(new SettingsScreen(mc.screen));
        return true;
    }
}
