package com.voidrun;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Voidrun: a fixed frame time benchmark. F7 starts a run, F8 opens the settings. */
public final class VoidrunMod implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("voidrun");

    @Override
    public void onInitializeClient() {
        VoidWorld.INSTANCE.cleanupStale();
        RunManager.INSTANCE.reloadConfig();
        KeybindHandler.register();
        LOG.info("Voidrun ready, F7 starts a run and F8 opens the settings");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeybindHandler.onClientTick(client);
            RunManager.INSTANCE.onClientTick();
        });

        LevelRenderEvents.END_MAIN.register(context -> RunManager.INSTANCE.onRenderFrame());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("voidrun", "overlay"),
                RunOverlay::render);
    }
}
