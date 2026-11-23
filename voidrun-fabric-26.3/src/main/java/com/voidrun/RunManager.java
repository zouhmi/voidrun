package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.GameType;

import java.io.File;

/** Owns the run state machine, the player lock and the result lifecycle (Fabric 26.x). */
public final class RunManager {
    public static final RunManager INSTANCE = new RunManager();

    private static final long NANOS_PER_SEC = 1000000000L;
    private static final long LOADING_TIMEOUT_NS = 90L * NANOS_PER_SEC;

    private RunState state = RunState.IDLE;
    private final FrameSampler sampler = new FrameSampler();
    private final UsageSampler usage = new UsageSampler();
    private VoidrunConfig config;

    private long phaseStartNs;
    private long readySinceNs;
    private long runStartNs;
    private boolean titleRun;
    private boolean focusLost;

    private boolean hasLock;
    private double lockX;
    private double lockY;
    private double lockZ;
    private float lockYaw;
    private float lockPitch;

    private boolean captured;
    private double savedX;
    private double savedY;
    private double savedZ;
    private float savedYaw;
    private float savedPitch;
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private double savedFallDistance;
    private boolean savedOnGround;
    private boolean savedAllowFlying;
    private boolean savedFlying;
    private boolean savedInvulnerable;
    private boolean savedAllowEdit;
    private boolean savedNoPhysics;
    private GameType savedGameType;

    private ResultData lastResult;

    private long liveLastNs;
    private double liveFrameMs = 16.6667;

    private RunManager() {
    }

    // --------------------------------------------------------------- settings

    public VoidrunConfig config() {
        if (config == null) {
            try {
                config = VoidrunConfig.load(Minecraft.getInstance().gameDirectory);
            } catch (Throwable t) {
                config = new VoidrunConfig();
            }
        }
        return config;
    }

    public void reloadConfig() {
        config = null;
        config();
    }

    // ------------------------------------------------------------------ state

    public RunState state() {
        return state;
    }

    public ResultData lastResult() {
        return lastResult;
    }

    public boolean isActive() {
        return state == RunState.LOADING || state == RunState.WARMUP || state == RunState.RUNNING;
    }

    public double liveFps() {
        return liveFrameMs > 0.0 ? 1000.0 / liveFrameMs : 0.0;
    }

    public double liveFrameMs() {
        return liveFrameMs;
    }

    // ------------------------------------------------------------------ start

    public void startRun(boolean fromTitle) {
        if (state != RunState.IDLE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        VoidrunConfig cfg = config();
        lastResult = null;
        hasLock = false;
        captured = false;
        readySinceNs = 0L;
        runStartNs = 0L;
        focusLost = false;
        liveFrameMs = 16.6667;
        liveLastNs = 0L;
        sampler.reset();
        sampler.setWindowSec(cfg.durationSeconds);
        usage.reset();
        RunOverlay.reset();

        titleRun = fromTitle && mc.level == null;
        if (!titleRun) {
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                return;
            }
            capture(player, mc);
            lockX = Math.floor(player.getX()) + 0.5;
            lockZ = Math.floor(player.getZ()) + 0.5;
            lockY = Math.max(1.0, Math.min(cfg.spawnHeight, mc.level.getMaxY() - 20.0));
            lockYaw = cfg.cameraYaw;
            lockPitch = cfg.cameraPitch;
            hasLock = true;
            readySinceNs = System.nanoTime();
        } else if (!VoidWorld.INSTANCE.ensureBenchmarkWorld()) {
            chat(mc, "Voidrun: could not open the benchmark world");
            return;
        }

        state = RunState.LOADING;
        phaseStartNs = System.nanoTime();
    }

    // ------------------------------------------------------------------- tick

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();

        if (isActive()) {
            if (!isWindowActive()) {
                focusLost = true;
            }
            if (mc.gui.screen() instanceof PauseScreen) {
                if (!isWindowActive()) {
                    // The client pauses single player when it loses focus. Close the menu so the
                    // frames keep coming, and record the interruption for the results screen.
                    focusLost = true;
                    mc.gui.setScreen(null);
                } else {
                    requestAbort();
                    return;
                }
            }
        }

        if (state == RunState.LOADING) {
            tickLoading(mc, now);
        } else if (state == RunState.WARMUP) {
            LocalPlayer player = mc.player;
            if (player == null) {
                abort();
                return;
            }
            lock(player, mc);
            if (now - phaseStartNs >= warmupNs()) {
                startRunning(now);
            }
        } else if (state == RunState.RUNNING) {
            LocalPlayer player = mc.player;
            if (player == null) {
                abort();
                return;
            }
            lock(player, mc);
            long guard = nanos(config().durationSeconds + UiSpec.GUARD_EXTRA_SECONDS);
            if (runStartNs > 0L && now - runStartNs > guard) {
                finishRun(now);
            }
        } else if (state == RunState.FINISHED) {
            LocalPlayer player = mc.player;
            if (player != null) {
                lock(player, mc);
            }
        }
    }

    private void tickLoading(Minecraft mc, long now) {
        VoidrunConfig cfg = config();
        if (titleRun) {
            if (mc.player != null && mc.level != null) {
                if (readySinceNs == 0L) {
                    readySinceNs = now;
                    LocalPlayer player = mc.player;
                    lockX = Math.floor(player.getX()) + 0.5;
                    lockZ = Math.floor(player.getZ()) + 0.5;
                    lockY = cfg.spawnHeight;
                    lockYaw = cfg.cameraYaw;
                    lockPitch = cfg.cameraPitch;
                    hasLock = true;
                }
            } else if (now - phaseStartNs > LOADING_TIMEOUT_NS) {
                chat(mc, "Voidrun: the benchmark world did not load in time");
                abort();
                return;
            }
        }
        if (mc.player != null) {
            lock(mc.player, mc);
        }
        if (readySinceNs != 0L && now - readySinceNs >= (long) (UiSpec.LOADING_SECONDS * NANOS_PER_SEC)) {
            state = RunState.WARMUP;
            phaseStartNs = now;
        }
    }

    private void startRunning(long now) {
        state = RunState.RUNNING;
        phaseStartNs = now;
        runStartNs = now;
        liveLastNs = 0L;
        sampler.reset();
        sampler.setWindowSec(config().durationSeconds);
        usage.reset();
    }

    // ----------------------------------------------------------------- frames

    public void onRenderFrame() {
        long now = System.nanoTime();
        if (state != RunState.WARMUP && state != RunState.RUNNING) {
            return;
        }
        if (liveLastNs != 0L) {
            double deltaMs = (now - liveLastNs) / 1.0e6;
            if (deltaMs > 0.0 && deltaMs < 5000.0) {
                double alpha = 1.0 - Math.exp(-deltaMs / 500.0);
                liveFrameMs += (deltaMs - liveFrameMs) * alpha;
            }
        }
        liveLastNs = now;
        usage.sample(now);

        if (state == RunState.RUNNING) {
            if (sampler.hasBase() && sampler.elapsedSec(now) >= config().durationSeconds) {
                finishRun(now);
                return;
            }
            sampler.onFrame(now);
        }
    }

    private void finishRun(long now) {
        Minecraft mc = Minecraft.getInstance();
        VoidrunConfig cfg = config();
        double[] times = sampler.finish(now);
        ResultData data = StatsCalculator.compute(times, cfg.durationSeconds);
        data.timestampEpochMs = System.currentTimeMillis();
        data.benchmarkName = cfg.benchmarkName;
        data.modVersion = "1.1.0";
        data.mcVersion = "26.3";
        data.loader = "fabric";
        data.loaderVersion = "0.20.1";
        data.avgCpuPercent = usage.avgCpuPercent();
        data.avgRamMb = usage.avgRamMb();
        data.windowFocusLost = focusLost;
        try {
            data.vsyncOn = mc.options.enableVsync().get().booleanValue();
            data.fpsLimit = Math.max(0, mc.options.framerateLimit().get().intValue());
        } catch (Throwable ignored) {
            // graphics settings are best effort
        }
        PlatformInfo.fill(data.system, mc);

        String note;
        boolean noteError = false;
        if (!cfg.autoSave) {
            note = "Auto save is off, this run was not written to disk.";
        } else {
            try {
                File file = ResultExporter.save(mc.gameDirectory, cfg, data);
                note = "Saved to " + cfg.outputFolder + "/"
                        + (file == null ? data.runId : file.getName());
            } catch (Throwable t) {
                note = "Could not save results: " + message(t);
                noteError = true;
            }
        }
        if (cfg.autoCopy) {
            try {
                mc.keyboardHandler.setClipboard(data.summaryText());
            } catch (Throwable ignored) {
                // clipboard is best effort
            }
        }

        lastResult = data;
        state = RunState.FINISHED;
        mc.gui.setScreen(new ResultsScreen(data, note, noteError, cfg));
    }

    // ----------------------------------------------------------------- player

    private void lock(LocalPlayer player, Minecraft mc) {
        if (!hasLock) {
            return;
        }
        player.setPos(lockX, lockY, lockZ);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        player.setOnGround(false);
        player.setYRot(lockYaw);
        player.setXRot(lockPitch);
        player.yRotO = lockYaw;
        player.xRotO = lockPitch;
        player.yHeadRot = lockYaw;
        player.yHeadRotO = lockYaw;
        player.noPhysics = true;
        player.setNoGravity(true);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().invulnerable = true;
        player.getAbilities().mayBuild = false;
        player.input.keyPresses = Input.EMPTY;
        if (mc.level != null) {
            // 26.x moved world time into a clock manager; rain is enough to keep the scene stable.
            mc.level.setRainLevel(0.0f);
            mc.level.setThunderLevel(0.0f);
        }
    }

    private void capture(LocalPlayer player, Minecraft mc) {
        captured = true;
        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedYaw = player.getYRot();
        savedPitch = player.getXRot();
        savedMotionX = player.getDeltaMovement().x;
        savedMotionY = player.getDeltaMovement().y;
        savedMotionZ = player.getDeltaMovement().z;
        savedFallDistance = player.fallDistance;
        savedOnGround = player.onGround();
        savedAllowFlying = player.getAbilities().mayfly;
        savedFlying = player.getAbilities().flying;
        savedInvulnerable = player.getAbilities().invulnerable;
        savedAllowEdit = player.getAbilities().mayBuild;
        savedNoPhysics = player.noPhysics;
        savedGameType = mc.gameMode == null ? null : mc.gameMode.getPlayerMode();
    }

    private void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (titleRun) {
            VoidWorld.INSTANCE.leaveBenchmarkWorld();
            captured = false;
            return;
        }
        if (!captured) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setPos(savedX, savedY, savedZ);
            player.setYRot(savedYaw);
            player.setXRot(savedPitch);
            player.setDeltaMovement(savedMotionX, savedMotionY, savedMotionZ);
            player.fallDistance = savedFallDistance;
            player.setOnGround(savedOnGround);
            player.noPhysics = savedNoPhysics;
            player.setNoGravity(false);
            player.getAbilities().mayfly = savedAllowFlying;
            player.getAbilities().flying = savedFlying;
            player.getAbilities().invulnerable = savedInvulnerable;
            player.getAbilities().mayBuild = savedAllowEdit;
        }
        if (savedGameType != null && mc.gameMode != null) {
            mc.gameMode.setLocalMode(savedGameType);
        }
        captured = false;
    }

    // ---------------------------------------------------------------- dismiss

    /** Esc during a run: asks first when the setting is on. */
    public void requestAbort() {
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof AbortConfirmScreen) {
            return;
        }
        if (mc.gui.screen() instanceof PauseScreen) {
            mc.gui.setScreen(null);
        }
        if (config().confirmCancel) {
            mc.gui.setScreen(new AbortConfirmScreen());
        } else {
            abort();
        }
    }

    public void abort() {
        if (!isActive()) {
            return;
        }
        state = RunState.IDLE;
        sampler.reset();
        hasLock = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof PauseScreen) {
            mc.gui.setScreen(null);
        }
        restore();
    }

    public void dismissResults(boolean again) {
        if (state != RunState.FINISHED) {
            return;
        }
        boolean wasTitle = titleRun;
        state = RunState.IDLE;
        hasLock = false;
        sampler.reset();
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof ResultsScreen || mc.gui.screen() instanceof SettingsScreen) {
            mc.gui.setScreen(null);
        }
        restore();
        if (again) {
            startRun(wasTitle);
        }
    }

    // --------------------------------------------------------------- progress

    public double progress() {
        long now = System.nanoTime();
        if (state == RunState.WARMUP) {
            double warmup = Math.max(0, config().warmupSeconds);
            return warmup <= 0.0 ? 1.0 : clamp01((now - phaseStartNs) / (warmup * NANOS_PER_SEC));
        }
        if (state == RunState.RUNNING) {
            return clamp01(sampler.elapsedSec(now) / Math.max(1, config().durationSeconds));
        }
        if (state == RunState.LOADING && readySinceNs != 0L) {
            return clamp01((now - readySinceNs) / (UiSpec.LOADING_SECONDS * NANOS_PER_SEC));
        }
        return 0.0;
    }

    public double phaseRemainingSec() {
        if (state == RunState.WARMUP) {
            double elapsed = (System.nanoTime() - phaseStartNs) / 1.0e9;
            return Math.max(0.0, config().warmupSeconds - elapsed);
        }
        if (state == RunState.RUNNING) {
            return Math.max(0.0, config().durationSeconds - sampler.elapsedSec(System.nanoTime()));
        }
        return 0.0;
    }

    public boolean warmupPhase() {
        return state == RunState.WARMUP;
    }

    // ---------------------------------------------------------------- helpers

    private long warmupNs() {
        return nanos(Math.max(0, config().warmupSeconds));
    }

    private static long nanos(long seconds) {
        return seconds * NANOS_PER_SEC;
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    private static String message(Throwable t) {
        String m = t.getMessage();
        return m == null || m.length() == 0 ? t.getClass().getSimpleName() : m;
    }

    private boolean isWindowActive() {
        try {
            return Minecraft.getInstance().getWindow().isFocused();
        } catch (Throwable t) {
            return true;
        }
    }

    private static void chat(Minecraft mc, String message) {
        try {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(message));
            }
        } catch (Throwable ignored) {
            // chat is best effort
        }
    }
}
