package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * F7 starts a run, F8 opens the settings. Both work on the title screen.
 *
 * <p>Keys are read from the key event queue, from the keybinding click counter and, as a last
 * resort, from polled key state. Launchers that replace the input backend can deliver key
 * callbacks normally while polled state stays "not pressed", which would leave the keys dead.
 */
public final class KeybindHandler {
    private static KeyBinding startKey;
    private static KeyBinding settingsKey;
    private static boolean startDown;
    private static boolean settingsDown;
    private static boolean escDown;

    private KeybindHandler() {
    }

    public static void register() {
        startKey = new KeyBinding("key.voidrun.start", Keyboard.KEY_F7, "Voidrun");
        settingsKey = new KeyBinding("key.voidrun.settings", Keyboard.KEY_F8, "Voidrun");
        ClientRegistry.registerKeyBinding(startKey);
        ClientRegistry.registerKeyBinding(settingsKey);
    }

    /** Runs for every key event, in menus and in the world alike. */
    public static void onKeyInput() {
        if (Keyboard.isRepeatEvent() || !Keyboard.getEventKeyState()) {
            return;
        }
        int key = Keyboard.getEventKey();
        if (startKey != null && key == startKey.getKeyCode()) {
            tryStart();
        } else if (settingsKey != null && key == settingsKey.getKeyCode()) {
            trySettings();
        } else if (key == Keyboard.KEY_ESCAPE && RunManager.INSTANCE.isActive()) {
            RunManager.INSTANCE.requestAbort();
        }
    }

    public static void onClientTick() {
        while (startKey != null && startKey.isPressed()) {
            if (tryStart()) {
                break;
            }
        }
        while (settingsKey != null && settingsKey.isPressed()) {
            if (trySettings()) {
                break;
            }
        }

        boolean start = startKey != null && Keyboard.isKeyDown(startKey.getKeyCode());
        if (start && !startDown) {
            tryStart();
        }
        startDown = start;

        boolean settings = settingsKey != null && Keyboard.isKeyDown(settingsKey.getKeyCode());
        if (settings && !settingsDown) {
            trySettings();
        }
        settingsDown = settings;

        boolean esc = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
        if (esc && !escDown && RunManager.INSTANCE.isActive()) {
            RunManager.INSTANCE.requestAbort();
        }
        escDown = esc;
    }

    private static boolean tryStart() {
        if (RunManager.INSTANCE.state() != RunState.IDLE) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean fromTitle = mc.thePlayer == null || mc.theWorld == null;
        RunManager.INSTANCE.startRun(fromTitle);
        return true;
    }

    private static boolean trySettings() {
        if (RunManager.INSTANCE.state() != RunState.IDLE) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof SettingsScreen) {
            return false;
        }
        mc.displayGuiScreen(new SettingsScreen(mc.currentScreen));
        return true;
    }
}
