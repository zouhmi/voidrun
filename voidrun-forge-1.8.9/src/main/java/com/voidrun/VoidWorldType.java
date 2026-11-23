package com.voidrun;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;

/**
 * A world type with no layers at all. Vanilla's flat type always adds grass and dirt, so a plain
 * {@code WorldType.FLAT} world is not empty and would put terrain into every benchmark run.
 *
 * <p>Constructing one of these registers it with {@link WorldType}, which has to happen before any
 * saved world is read, hence {@link #register()}.
 */
public final class VoidWorldType extends WorldType {
    /** Flat generator string: version 3, zero layers of air, plains biome. */
    private static final String EMPTY_SETTINGS = "3;0*minecraft:air;1";

    private static VoidWorldType instance;

    private VoidWorldType() {
        super("voidrun_void");
    }

    public static void register() {
        if (instance == null) {
            instance = new VoidWorldType();
        }
    }

    public static WorldType get() {
        register();
        return instance;
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new ChunkProviderFlat(world, world.getSeed(), false, EMPTY_SETTINGS);
    }
}
