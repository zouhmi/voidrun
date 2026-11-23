package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.File;

/**
 * Creates and joins the benchmark world used by title-screen runs. The save is deleted before
 * every run and again when the run ends, so nothing is left behind after a cancel or a crash.
 */
public final class VoidWorld {
    public static final VoidWorld INSTANCE = new VoidWorld();
    public static final String FOLDER = "voidrun_benchmark";
    public static final String NAME = "Voidrun Benchmark";
    private static final String LEGACY_FOLDER = "Voidrun Benchmark";
    private static final long SEED = 0x564F494452554EL;

    private static boolean openedThisSession;

    private VoidWorld() {
    }

    /** Removes benchmark saves left behind by a crash or an older build. */
    public void cleanupStale() {
        deleteLevel(LEGACY_FOLDER);
        deleteLevel(FOLDER);
    }

    public boolean ensureBenchmarkWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (isInBenchmarkWorld()) {
            return true;
        }
        cleanupStale();
        LevelSettings settings = new LevelSettings(NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(SEED, false, false);
        try {
            mc.createWorldOpenFlows().createFreshLevel(FOLDER, settings, options,
                    WorldPresets::createFlatWorldDimensions, new TitleScreen());
        } catch (Throwable t) {
            return false;
        }
        openedThisSession = true;
        return true;
    }

    public boolean isInBenchmarkWorld() {
        Minecraft mc = Minecraft.getInstance();
        return openedThisSession && mc.level != null && mc.getSingleplayerServer() != null;
    }

    public void leaveBenchmarkWorld() {
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.disconnect(new TitleScreen(), false);
        } catch (Throwable ignored) {
            // already back on the title screen
        }
        openedThisSession = false;
        deleteLevel(FOLDER);
    }

    private static void deleteLevel(String name) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getLevelSource().levelExists(name)) {
                deleteTree(mc.getLevelSource().getLevelPath(name).toFile());
            }
        } catch (Throwable ignored) {
            // a locked file is retried by cleanupStale() on the next start
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteTree(child);
                }
            }
        }
        file.delete();
    }
}
