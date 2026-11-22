package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.core.LevelWeather;
import com.paulorchard.islandcraft.weatherofarrakis.core.Params;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import com.paulorchard.islandcraft.weatherofarrakis.core.WeatherEvent;
import com.paulorchard.islandcraft.weatherofarrakis.net.WeatherPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Drives the weather of every qualifying level on the server thread, sends the effective weather to clients and announces phases. */
public final class WeatherManager {
    private static final Random RNG = new Random();
    private static final Map<String, WeatherPayload> LAST_SENT = new HashMap<>();

    private WeatherManager() {}

    public static LevelWeather state(ServerLevel level) {
        return WeatherData.get(level.getServer()).of(level.dimension().identifier().toString());
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !WeatherConfig.isQualifying(level)) return;
        WeatherData data = WeatherData.get(level.getServer());
        LevelWeather w = data.of(level.dimension().identifier().toString());
        Params p = WeatherConfig.params();
        List<WeatherEvent> events = w.tick(p, level.getOverworldClockTime(), RNG);
        data.setDirty();
        dispatch(level, w, events);
        sync(level, w);
    }

    /** Applies the events of one tick or one command: chat, listeners. */
    public static void dispatch(ServerLevel level, LevelWeather w, List<WeatherEvent> events) {
        for (WeatherEvent e : events) {
            if (e instanceof WeatherEvent.Warning warn) {
                broadcast(level, warningMessage(w, warn));
            } else if (e instanceof WeatherEvent.PhaseChanged pc) {
                if (pc.to() == Phase.STORM) broadcast(level, Component.translatable("islandcraft_weatherofarrakis.coriolis.arrival", name(w.major)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                if (pc.from() == Phase.STORM && pc.to() == Phase.CLEARING) broadcast(level, Component.translatable("islandcraft_weatherofarrakis.coriolis.end", name(w.major)).withStyle(ChatFormatting.GREEN));
                StormQuery.firePhaseChange(level, pc.from(), pc.to());
            }
        }
    }

    private static Component name(boolean major) {
        return Component.translatable(major ? "islandcraft_weatherofarrakis.name.major" : "islandcraft_weatherofarrakis.name.coriolis");
    }

    public static Component timeText(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int m = seconds / 60;
            return Component.translatable(m == 1 ? "islandcraft_weatherofarrakis.time.minute" : "islandcraft_weatherofarrakis.time.minutes", m);
        }
        return Component.translatable(seconds == 1 ? "islandcraft_weatherofarrakis.time.second" : "islandcraft_weatherofarrakis.time.seconds", seconds);
    }

    private static Component warningMessage(LevelWeather w, WeatherEvent.Warning warn) {
        net.minecraft.network.chat.MutableComponent msg;
        if (warn.first()) {
            msg = Component.translatable("islandcraft_weatherofarrakis.coriolis.warning.first", name(w.major),
                    Component.translatable("islandcraft_weatherofarrakis.wind." + WeatherConfig.WIND_FROM.get().toLowerCase(java.util.Locale.ROOT)), timeText(warn.secondsLeft()));
        } else if (warn.secondsLeft() <= 30) {
            msg = Component.translatable("islandcraft_weatherofarrakis.coriolis.warning.final", name(w.major), timeText(warn.secondsLeft()));
        } else {
            msg = Component.translatable("islandcraft_weatherofarrakis.coriolis.warning", name(w.major), timeText(warn.secondsLeft()));
        }
        return msg.withStyle(ChatFormatting.GOLD);
    }

    public static void broadcast(ServerLevel level, Component msg) {
        for (ServerPlayer pl : level.players()) pl.sendSystemMessage(msg);
    }

    /** The message a player gets on joining during APPROACH or STORM. */
    public static void sendJoinMessage(ServerPlayer player, LevelWeather w) {
        if (w.phase == Phase.APPROACH) {
            player.sendSystemMessage(Component.translatable("islandcraft_weatherofarrakis.coriolis.join.approach", name(w.major), timeText(Math.max(1, (w.phaseTicksLeft() + 19) / 20))).withStyle(ChatFormatting.GOLD));
        } else if (w.phase == Phase.STORM) {
            player.sendSystemMessage(Component.translatable("islandcraft_weatherofarrakis.coriolis.join.storm", name(w.major)).withStyle(ChatFormatting.RED));
        }
    }

    // ---- client sync ----

    public static WeatherPayload effective(ServerLevel level, LevelWeather w) {
        float[] wind = WeatherConfig.windVector();
        float fraction = WeatherConfig.STAGE_BLEND_FRACTION.get().floatValue();
        Params p = WeatherConfig.params();
        switch (w.phase) {
            case APPROACH -> {
                int stage = w.approachStage();
                return new WeatherPayload(WeatherPayload.APPROACH, stage, 1f, wind[0], wind[1], fraction * w.stageLengthTicks(stage) / 20f, w.major);
            }
            case STORM, MAJOR_RESET, MAJOR_WAIT -> {
                return new WeatherPayload(WeatherPayload.STORM, 0, 1f, wind[0], wind[1], fraction * w.stageLengthTicks(6) / 20f, w.major);
            }
            case CLEARING -> {
                return new WeatherPayload(WeatherPayload.NONE, 0, 0f, wind[0], wind[1], w.clearingTicks / 20f, false);
            }
            default -> {
                float ramp = p.sandRampTicks() / 20f;
                if (w.sandActive && w.sandTicksLeft > p.sandRampTicks()) return new WeatherPayload(WeatherPayload.SANDSTORM, 0, 1f, wind[0], wind[1], ramp, false);
                return new WeatherPayload(WeatherPayload.NONE, 0, 0f, wind[0], wind[1], ramp, false);
            }
        }
    }

    private static boolean same(WeatherPayload a, WeatherPayload b) {
        return a != null && a.kind() == b.kind() && a.stage() == b.stage() && a.major() == b.major() && a.intensity() == b.intensity();
    }

    public static void sync(ServerLevel level, LevelWeather w) {
        WeatherPayload now = effective(level, w);
        String key = level.dimension().identifier().toString();
        if (same(LAST_SENT.get(key), now)) return;
        LAST_SENT.put(key, now);
        PacketDistributor.sendToPlayersInDimension(level, now);
    }

    /** Sends the weather of the player's level at once (join, dimension change); a non-qualifying level gets "none". */
    public static void syncPlayer(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (!WeatherConfig.isQualifying(level)) {
            PacketDistributor.sendToPlayer(player, new WeatherPayload(WeatherPayload.NONE, 0, 0f, 1f, 0f, 0f, false));
            return;
        }
        LevelWeather w = state(level);
        WeatherPayload now = effective(level, w);
        PacketDistributor.sendToPlayer(player, new WeatherPayload(now.kind(), now.stage(), now.intensity(), now.windX(), now.windZ(), 0f, now.major()));
    }

    public static void onServerStopped() {
        LAST_SENT.clear();
    }

    // ---- control (commands) ----

    public static boolean startCoriolis(ServerLevel level, int approachSeconds, int stormSeconds) {
        if (!WeatherConfig.isQualifying(level)) return false;
        LevelWeather w = state(level);
        if (w.phase != Phase.CALM) return false;
        List<WeatherEvent> ev = new java.util.ArrayList<>();
        w.startApproach(approachSeconds * 20, stormSeconds * 20, false, ev);
        WeatherData.get(level.getServer()).setDirty();
        dispatch(level, w, ev);
        sync(level, w);
        return true;
    }

    public static void stopCoriolis(ServerLevel level) {
        LevelWeather w = state(level);
        List<WeatherEvent> ev = new java.util.ArrayList<>();
        w.stop(ev);
        WeatherData.get(level.getServer()).setDirty();
        dispatch(level, w, ev);
        sync(level, w);
    }

    public static boolean startSandstorm(ServerLevel level, int minutes) {
        if (!WeatherConfig.isQualifying(level)) return false;
        LevelWeather w = state(level);
        if (w.phase != Phase.CALM || w.sandActive) return false;
        List<WeatherEvent> ev = new java.util.ArrayList<>();
        w.startSandstorm(minutes * 1200, ev);
        WeatherData.get(level.getServer()).setDirty();
        sync(level, w);
        return true;
    }

    public static void stopSandstorm(ServerLevel level) {
        LevelWeather w = state(level);
        List<WeatherEvent> ev = new java.util.ArrayList<>();
        w.stopSandstorm(ev);
        WeatherData.get(level.getServer()).setDirty();
        sync(level, w);
    }
}
