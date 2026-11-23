package com.voidrun;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Voidrun: a fixed frame time benchmark. F7 starts a run, F8 opens the settings. */
@Mod(modid = "voidrun", name = "Voidrun", version = "1.1.0", acceptableRemoteVersions = "*")
public final class VoidrunMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            Hook.register();
        }
    }

    /** Client-only hooks, kept separate so a dedicated server never loads GUI classes. */
    public static final class Hook {
        private Hook() {
        }

        static void register() {
            VoidWorldType.register();
            VoidWorld.INSTANCE.cleanupStale();
            RunManager.INSTANCE.reloadConfig();
            KeybindHandler.register();
            MinecraftForge.EVENT_BUS.register(new Hook());
        }

        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            KeybindHandler.onKeyInput();
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            KeybindHandler.onClientTick();
            RunManager.INSTANCE.onClientTick();
        }

        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            RunManager.INSTANCE.onRenderFrame();
        }

        @SubscribeEvent
        public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
            if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
                RunOverlay.render(event);
            }
        }
    }
}
