package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldSettings;

/**
 * Creates and joins the empty benchmark world used by title-screen runs. The save folder is
 * deleted before every run and again when the run ends, so a world from an older build is never
 * reused and nothing is left behind after a cancel or a crash.
 */
public final class VoidWorld {
    public static final VoidWorld INSTANCE = new VoidWorld();
    public static final String FOLDER = "voidrun_benchmark";
    public static final String NAME = "Voidrun Benchmark";
    private static final String LEGACY_FOLDER = "Voidrun Benchmark";
    private static final long SEED = 0x564F494452554EL;

    private VoidWorld() {
    }

    /** Removes benchmark saves left behind by a crash or an older build. */
    public void cleanupStale() {
        deleteSave(LEGACY_FOLDER);
        deleteSave(FOLDER);
    }

    public boolean ensureBenchmarkWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        if (isInBenchmarkWorld()) {
            return true;
        }
        cleanupStale();
        VoidWorldType.register();
        WorldSettings settings = new WorldSettings(SEED, WorldSettings.GameType.CREATIVE, false, false,
                VoidWorldType.get());
        settings.enableCommands();
        settings.setWorldName(NAME);
        try {
            mc.launchIntegratedServer(FOLDER, NAME, settings);
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    public boolean isInBenchmarkWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getIntegratedServer() != null && FOLDER.equals(mc.getIntegratedServer().getFolderName());
    }

    public void leaveBenchmarkWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.loadWorld((WorldClient) null);
        } catch (Throwable ignored) {
            // already back on the title screen
        }
        deleteSave(FOLDER);
    }

    private static void deleteSave(String folder) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getSaveLoader().canLoadWorld(folder)) {
                mc.getSaveLoader().deleteWorldDirectory(folder);
            }
        } catch (Throwable ignored) {
            // locked files are retried by cleanupStale() on the next start
        }
    }
}
