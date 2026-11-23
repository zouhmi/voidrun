package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.Display;

import java.io.File;

/** Owns the run state machine, the player lock and the result lifecycle (Forge 1.8.9). */
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
    private float savedFallDistance;
    private boolean savedOnGround;
    private boolean savedAllowFlying;
    private boolean savedFlying;
    private boolean savedInvulnerable;
    private boolean savedAllowEdit;
    private boolean savedNoClip;
    private WorldSettings.GameType savedGameType;

    private ResultData lastResult;

    private long liveLastNs;
    private double liveFrameMs = 16.6667;

    private RunManager() {
    }

    // --------------------------------------------------------------- settings

    public VoidrunConfig config() {
        if (config == null) {
            try {
                config = VoidrunConfig.load(Minecraft.getMinecraft().mcDataDir);
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
        Minecraft mc = Minecraft.getMinecraft();
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

        titleRun = fromTitle && (mc.thePlayer == null || mc.theWorld == null);
        if (!titleRun) {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null || mc.theWorld == null) {
                return;
            }
            capture(player, mc);
            lockX = Math.floor(player.posX) + 0.5;
            lockZ = Math.floor(player.posZ) + 0.5;
            int top = mc.theWorld.getActualHeight();
            if (top <= 0) {
                top = 256;
            }
            lockY = Math.max(1.0, Math.min(cfg.spawnHeight, top - 20.0));
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
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();

        if (isActive()) {
            if (!isWindowActive()) {
                focusLost = true;
            }
            if (mc.currentScreen instanceof GuiIngameMenu) {
                if (!isWindowActive()) {
                    // The client pauses single player when it loses focus. Close the menu so the
                    // frames keep coming, and record the interruption for the results screen.
                    focusLost = true;
                    mc.displayGuiScreen(null);
                } else {
                    requestAbort();
                    return;
                }
            }
        }

        if (state == RunState.LOADING) {
            tickLoading(mc, now);
        } else if (state == RunState.WARMUP) {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) {
                abort();
                return;
            }
            lock(player, mc);
            if (now - phaseStartNs >= warmupNs()) {
                startRunning(now);
            }
        } else if (state == RunState.RUNNING) {
            EntityPlayerSP player = mc.thePlayer;
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
            EntityPlayerSP player = mc.thePlayer;
            if (player != null) {
                lock(player, mc);
            }
        }
    }

    private void tickLoading(Minecraft mc, long now) {
        VoidrunConfig cfg = config();
        if (titleRun) {
            if (mc.thePlayer != null && mc.theWorld != null) {
                if (readySinceNs == 0L) {
                    readySinceNs = now;
                    EntityPlayerSP player = mc.thePlayer;
                    lockX = Math.floor(player.posX) + 0.5;
                    lockZ = Math.floor(player.posZ) + 0.5;
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
        if (mc.thePlayer != null) {
            lock(mc.thePlayer, mc);
        }
        if (readySinceNs != 0L && now - readySinceNs >= nanosDouble(UiSpec.LOADING_SECONDS)) {
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
        Minecraft mc = Minecraft.getMinecraft();
        VoidrunConfig cfg = config();
        double[] times = sampler.finish(now);
        ResultData data = StatsCalculator.compute(times, cfg.durationSeconds);
        data.timestampEpochMs = System.currentTimeMillis();
        data.benchmarkName = cfg.benchmarkName;
        data.modVersion = "1.1.0";
        data.mcVersion = "1.8.9";
        data.loader = "forge";
        data.loaderVersion = "11.15.1.2318";
        data.avgCpuPercent = usage.avgCpuPercent();
        data.avgRamMb = usage.avgRamMb();
        data.windowFocusLost = focusLost;
        try {
            data.vsyncOn = mc.gameSettings.enableVsync;
            data.fpsLimit = Math.max(0, mc.gameSettings.limitFramerate);
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
                File file = ResultExporter.save(mc.mcDataDir, cfg, data);
                note = "Saved to " + cfg.outputFolder + "/"
                        + (file == null ? data.runId : file.getName());
            } catch (Throwable t) {
                note = "Could not save results: " + message(t);
                noteError = true;
            }
        }
        if (cfg.autoCopy) {
            try {
                GuiScreen.setClipboardString(data.summaryText());
            } catch (Throwable ignored) {
                // clipboard is best effort
            }
        }

        lastResult = data;
        state = RunState.FINISHED;
        mc.displayGuiScreen(new ResultsScreen(data, note, noteError, cfg));
    }

    // ----------------------------------------------------------------- player

    private void lock(EntityPlayerSP player, Minecraft mc) {
        if (!hasLock) {
            return;
        }
        player.setPosition(lockX, lockY, lockZ);
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;
        player.fallDistance = 0.0f;
        player.onGround = false;
        player.rotationYaw = lockYaw;
        player.rotationPitch = lockPitch;
        player.rotationYawHead = lockYaw;
        player.setSneaking(false);
        player.setSprinting(false);
        player.noClip = true;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.capabilities.disableDamage = true;
        player.capabilities.allowEdit = false;
        if (player.movementInput != null) {
            player.movementInput.moveForward = 0.0f;
            player.movementInput.moveStrafe = 0.0f;
            player.movementInput.jump = false;
            player.movementInput.sneak = false;
        }
        if (mc.theWorld != null) {
            mc.theWorld.setWorldTime(6000L);
            if (mc.theWorld.getWorldInfo().isRaining()) {
                mc.theWorld.getWorldInfo().setRaining(false);
            }
        }
    }

    private void capture(EntityPlayerSP player, Minecraft mc) {
        captured = true;
        savedX = player.posX;
        savedY = player.posY;
        savedZ = player.posZ;
        savedYaw = player.rotationYaw;
        savedPitch = player.rotationPitch;
        savedMotionX = player.motionX;
        savedMotionY = player.motionY;
        savedMotionZ = player.motionZ;
        savedFallDistance = player.fallDistance;
        savedOnGround = player.onGround;
        savedAllowFlying = player.capabilities.allowFlying;
        savedFlying = player.capabilities.isFlying;
        savedInvulnerable = player.capabilities.disableDamage;
        savedAllowEdit = player.capabilities.allowEdit;
        savedNoClip = player.noClip;
        savedGameType = mc.playerController == null ? null : mc.playerController.getCurrentGameType();
    }

    private void restore() {
        Minecraft mc = Minecraft.getMinecraft();
        if (titleRun) {
            VoidWorld.INSTANCE.leaveBenchmarkWorld();
            captured = false;
            return;
        }
        if (!captured) {
            return;
        }
        EntityPlayerSP player = mc.thePlayer;
        if (player != null) {
            player.setPositionAndRotation(savedX, savedY, savedZ, savedYaw, savedPitch);
            player.motionX = savedMotionX;
            player.motionY = savedMotionY;
            player.motionZ = savedMotionZ;
            player.fallDistance = savedFallDistance;
            player.onGround = savedOnGround;
            player.noClip = savedNoClip;
            player.capabilities.allowFlying = savedAllowFlying;
            player.capabilities.isFlying = savedFlying;
            player.capabilities.disableDamage = savedInvulnerable;
            player.capabilities.allowEdit = savedAllowEdit;
        }
        if (savedGameType != null && mc.playerController != null) {
            mc.playerController.setGameType(savedGameType);
        }
        captured = false;
    }

    // ---------------------------------------------------------------- dismiss

    /** Esc during a run: asks first when the setting is on. */
    public void requestAbort() {
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof AbortConfirmScreen) {
            return;
        }
        if (mc.currentScreen instanceof GuiIngameMenu) {
            mc.displayGuiScreen(null);
        }
        if (config().confirmCancel) {
            mc.displayGuiScreen(new AbortConfirmScreen());
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiIngameMenu) {
            mc.displayGuiScreen(null);
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof ResultsScreen || mc.currentScreen instanceof SettingsScreen) {
            mc.displayGuiScreen(null);
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
            return clamp01((now - readySinceNs)
                    / (UiSpec.LOADING_SECONDS * NANOS_PER_SEC));
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

    private static long nanosDouble(double seconds) {
        return (long) (seconds * NANOS_PER_SEC);
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
            return Display.isActive();
        } catch (Throwable t) {
            return true;
        }
    }

    private static void chat(Minecraft mc, String message) {
        try {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(message));
            }
        } catch (Throwable ignored) {
            // chat is best effort
        }
    }
}
