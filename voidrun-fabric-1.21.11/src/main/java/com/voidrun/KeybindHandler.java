package com.voidrun;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
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
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("voidrun", "main"));
        startKey = new KeyMapping("key.voidrun.start", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7,
                category);
        settingsKey = new KeyMapping("key.voidrun.settings", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, category);
        KeyBindingHelper.registerKeyBinding(startKey);
        KeyBindingHelper.registerKeyBinding(settingsKey);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register((s, input) -> {
                    if (startKey.matches(input)) {
                        return !tryStart();
                    }
                    if (settingsKey.matches(input)) {
                        return !trySettings();
                    }
                    if (keyOf(input) == GLFW.GLFW_KEY_ESCAPE && RunManager.INSTANCE.isActive()) {
                        RunManager.INSTANCE.requestAbort();
                    }
                    return true;
                }));
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

        boolean esc = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_ESCAPE);
        if (esc && !escDown && RunManager.INSTANCE.isActive()) {
            if (mc.screen instanceof PauseScreen) {
                mc.setScreen(null);
            }
            RunManager.INSTANCE.requestAbort();
        }
        escDown = esc;
    }

    private static int keyOf(KeyEvent input) {
        return input.key();
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
