package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Game-bus events on the server side. */
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        WeatherCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post e) {
        WeatherManager.onLevelTick(e);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) joined(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) joined(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) WeatherManager.syncPlayer(p);
    }

    private static void joined(ServerPlayer p) {
        WeatherManager.syncPlayer(p);
        if (p.level() instanceof ServerLevel level && WeatherConfig.isQualifying(level)) {
            WeatherManager.sendJoinMessage(p, WeatherManager.state(level));
        }
    }

    @SubscribeEvent
    public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
        Exposure.onDeath(e);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        Exposure.forget(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        WeatherManager.onServerStopped();
    }
}
