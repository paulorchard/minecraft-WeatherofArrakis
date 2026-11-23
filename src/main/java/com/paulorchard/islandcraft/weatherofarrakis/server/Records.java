package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.core.ChunkRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Chunk records glue (Part 6). Counted as altered: a block a player places or breaks (NeoForge BlockEvent.EntityPlaceEvent with a player,
 * BlockEvent.BreakEvent) and a storm-death marker (skull or bone block; keyed on the placement, not on the block type). NOT counted: pistons, fluids, explosions, fire, plant growth, entity actions
 * (endermen, falling blocks), commands, and changes to container contents.
 */
public final class Records {
    private static int visitCountdown;

    private Records() {}

    public static ChunkRecords get(MinecraftServer server) {
        return WeatherData.get(server).records;
    }

    public static String dim(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public static void markAltered(ServerLevel level, ChunkPos pos) {
        if (!WeatherConfig.isQualifying(level)) return;
        WeatherData data = WeatherData.get(level.getServer());
        data.records.markAltered(dim(level), pos.pack());
        data.setDirty();
    }

    public static void onMarker(ServerLevel level, BlockPos pos) {
        markAltered(level, ChunkPos.containing(pos));
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer && e.getLevel() instanceof ServerLevel level) markAltered(level, ChunkPos.containing(e.getPos()));
    }

    public static void onBreak(BreakBlockEvent e) {
        if (e.getPlayer() instanceof ServerPlayer && e.getLevel() instanceof ServerLevel level) markAltered(level, ChunkPos.containing(e.getPos()));
    }

    /** Radius in chunks around a player whose altered chunks count as visited. */
    public static int visitRadius(MinecraftServer server) {
        int r = WeatherConfig.CHUNK_VISIT_RADIUS.get();
        return r > 0 ? r : server.getPlayerList().getSimulationDistance();
    }

    /**
     * Every chunk_visit_seconds: altered chunks within the radius of any player get last visit = now.
     * Cost per pass: players x (2r+1)^2 hash lookups (r = simulation distance, default 10: 441 per player), independent of how many chunks are recorded.
     */
    public static void tickVisits(MinecraftServer server) {
        if (--visitCountdown > 0) return;
        visitCountdown = WeatherConfig.CHUNK_VISIT_SECONDS.get() * 20;
        WeatherData data = WeatherData.get(server);
        int r = visitRadius(server);
        boolean any = false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel level = (ServerLevel) p.level();
            if (!WeatherConfig.isQualifying(level)) continue;
            String dim = dim(level);
            ChunkPos c = p.chunkPosition();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) any |= data.records.visit(dim, ChunkPos.pack(c.x() + dx, c.z() + dz));
        }
        if (any) data.setDirty();
    }

    /** Logout: the player's record stores the counter now (not while a Major storm is kicking everyone). */
    public static void onLogout(ServerPlayer p) {
        if (MajorStorm.isRefusingLogins()) return;
        WeatherData data = WeatherData.get(p.level().getServer());
        data.records.players.put(p.getUUID(), data.records.counter);
        data.records.playerForce.put(p.getUUID(), data.records.forceSeq);
        data.setDirty();
    }
}
