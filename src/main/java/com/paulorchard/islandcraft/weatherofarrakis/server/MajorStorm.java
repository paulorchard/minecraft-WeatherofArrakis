package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.core.LevelWeather;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import com.paulorchard.islandcraft.weatherofarrakis.core.WeatherEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;

/**
 * The Major Coriolis storm. In memory only: a restart never resumes it (WeatherData turns a saved Major phase into CALM).
 * Sequence: LEAD (announced, counting down) -> STORM (ordinary APPROACH + STORM with the major flag) -> everyone kicked, counter raised,
 * PASS (chunk reset within the window) -> WAIT (logins refused until the reopen time) -> CALM.
 */
public final class MajorStorm {
    private enum Stage { IDLE, LEAD, STORM, PASS, WAIT }

    private static final Random RNG = new Random();
    private static Stage stage = Stage.IDLE;
    private static int leadTicks;
    private static int stormSeconds;
    private static int windowSeconds;
    private static int sinceDisconnect;
    private static boolean refusing;
    private static ChunkReset pass;
    private static int passWindowTicks;
    private static int passTicks;

    /** A manual "reset all" / "reset here" run, ticked beside the pass. */
    private static ChunkReset manual;
    private static CommandSourceStack manualSource;

    private MajorStorm() {}

    public static boolean isRefusingLogins() {
        return refusing;
    }

    public static boolean isRunning() {
        return stage != Stage.IDLE;
    }

    public static String describe() {
        return stage.name();
    }

    private static List<ServerLevel> levels(MinecraftServer server) {
        List<ServerLevel> out = new ArrayList<>();
        for (ServerLevel l : server.getAllLevels()) if (WeatherConfig.isQualifying(l)) out.add(l);
        return out;
    }

    private static void announce(MinecraftServer server, Component msg) {
        for (ServerLevel l : levels(server)) WeatherManager.broadcast(l, msg);
    }

    public static boolean start(MinecraftServer server, int minutes, int stormSecs, int windowSecs) {
        if (stage != Stage.IDLE || levels(server).isEmpty()) return false;
        stormSeconds = stormSecs > 0 ? stormSecs : WeatherConfig.MAJOR_STORM_SECONDS.get();
        windowSeconds = windowSecs > 0 ? windowSecs : WeatherConfig.MAJOR_RESET_WINDOW.get();
        leadTicks = minutes * 1200;
        stage = Stage.LEAD;
        announce(server, Component.translatable("islandcraft_weatherofarrakis.major.announce", WeatherManager.timeText(minutes * 60)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        beginApproachIfDue(server);
        return true;
    }

    /** Calls the storm off; only before players are disconnected. Returns false if it is too late or nothing was planned. */
    public static boolean cancel(MinecraftServer server) {
        if (stage != Stage.LEAD && stage != Stage.STORM) return false;
        for (ServerLevel l : levels(server)) {
            LevelWeather w = WeatherManager.state(l);
            if (w.major && w.phase != Phase.CALM) WeatherManager.stopCoriolis(l);
        }
        stage = Stage.IDLE;
        announce(server, Component.translatable("islandcraft_weatherofarrakis.major.cancelled").withStyle(ChatFormatting.GREEN));
        return true;
    }

    private static void beginApproachIfDue(MinecraftServer server) {
        int approach = WeatherConfig.APPROACH_SECONDS.get() * 20;
        if (leadTicks > approach) return;
        for (ServerLevel l : levels(server)) {
            LevelWeather w = WeatherManager.state(l);
            if (w.phase != Phase.CALM) WeatherManager.stopCoriolis(l);
            List<WeatherEvent> ev = new ArrayList<>();
            w.startApproach(Math.max(20, leadTicks), stormSeconds * 20, true, ev);
            WeatherData.get(server).setDirty();
            WeatherManager.dispatch(l, w, ev);
            WeatherManager.sync(l, w);
        }
        stage = Stage.STORM;
    }

    public static void tick(MinecraftServer server) {
        if (manual != null) {
            manual.tick();
            if (manual.isFinished()) finishManual(server);
        }
        switch (stage) {
            case LEAD -> {
                if (--leadTicks % 12000 == 0 && leadTicks > WeatherConfig.APPROACH_SECONDS.get() * 20) {
                    announce(server, Component.translatable("islandcraft_weatherofarrakis.major.reminder", WeatherManager.timeText(leadTicks / 20)).withStyle(ChatFormatting.GOLD));
                }
                beginApproachIfDue(server);
            }
            case STORM -> {
                for (ServerLevel l : levels(server)) if (WeatherManager.state(l).phase == Phase.MAJOR_RESET) {
                    beginPass(server);
                    break;
                }
            }
            case PASS -> {
                sinceDisconnect++;
                passTicks++;
                pass.tick();
                if (pass.isFinished() || passTicks >= passWindowTicks) endPass(server);
            }
            case WAIT -> {
                if (++sinceDisconnect >= WeatherConfig.MAJOR_LOGIN_REOPEN.get() * 20) reopen(server);
            }
            default -> { }
        }
    }

    private static void beginPass(MinecraftServer server) {
        WeatherData data = WeatherData.get(server);
        boolean kick = WeatherConfig.MAJOR_DISCONNECTS.get();
        // Players' records are written first (their counter before this storm), so the rescue can compare against the ledger.
        for (ServerPlayer p : server.getPlayerList().getPlayers()) data.records.players.put(p.getUUID(), data.records.counter);
        if (kick) {
            refusing = true;
            for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
                p.connection.disconnect(Component.translatable("islandcraft_weatherofarrakis.major.kick"));
            }
        }
        data.records.counter++;
        data.setDirty();
        save(server);
        passWindowTicks = windowSeconds * 20;
        passTicks = 0;
        sinceDisconnect = 0;
        pass = new ChunkReset(server, windowSeconds, false);
        stage = Stage.PASS;
        WeatherOfArrakis.LOGGER.info("Major storm: counter is now {}, reset pass started ({} chunks eligible, window {} s, disconnects {})",
                data.records.counter, pass.stats.checked + 0, windowSeconds, kick);
    }

    private static void save(MinecraftServer server) {
        try {
            server.saveEverything(true, false, false);
        } catch (Throwable t) {
            WeatherOfArrakis.LOGGER.warn("Could not save around the reset pass: {}", t.toString());
        }
    }

    private static void endPass(MinecraftServer server) {
        pass.stopIssuing();
        WeatherOfArrakis.LOGGER.info(pass.summary());
        pass = null;
        save(server);
        for (ServerLevel l : levels(server)) {
            LevelWeather w = WeatherManager.state(l);
            List<WeatherEvent> ev = new ArrayList<>();
            w.toMajorWait(ev);
            WeatherManager.dispatch(l, w, ev);
        }
        if (!WeatherConfig.MAJOR_DISCONNECTS.get()) {
            reopen(server);
        } else {
            stage = Stage.WAIT;
        }
    }

    private static void reopen(MinecraftServer server) {
        refusing = false;
        long clockNow = server.overworld().getOverworldClockTime();
        for (ServerLevel l : levels(server)) {
            LevelWeather w = WeatherManager.state(l);
            List<WeatherEvent> ev = new ArrayList<>();
            w.endMajor(com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig.params(), clockNow, RNG, ev);
            WeatherData.get(server).setDirty();
            WeatherManager.dispatch(l, w, ev);
            WeatherManager.sync(l, w);
        }
        stage = Stage.IDLE;
        WeatherOfArrakis.LOGGER.info("Major storm over: logins open, phase CALM");
    }

    public static void onServerStopping(MinecraftServer server) {
        if (pass != null) {
            pass.stopIssuing();
            WeatherOfArrakis.LOGGER.info(pass.summary() + " (server stopping)");
            pass = null;
        }
        if (manual != null) manual.stopIssuing();
        refusing = false;
        stage = Stage.IDLE;
    }

    // ---- manual resets ----

    public static boolean startManual(MinecraftServer server, CommandSourceStack source, ChunkReset run) {
        if (manual != null || pass != null) return false;
        manual = run;
        manualSource = source;
        return true;
    }

    private static void finishManual(MinecraftServer server) {
        String msg = manual.summary();
        CommandSourceStack src = manualSource;
        manual = null;
        manualSource = null;
        WeatherOfArrakis.LOGGER.info(msg);
        if (src != null) src.sendSuccess(() -> Component.literal(msg), true);
    }
}
