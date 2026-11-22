package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.core.ChunkRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Login rescue. Route used: (a) in the login event the chunk is loaded to FULL on the server thread (this generates it if it was reset),
 * the surface is read and the player is moved before their first tick, so they never suffocate or fall.
 * Rule 1: the ledger says the chunk was reset after the player's recorded logout. Rule 2: applies even in an open cave (the way in is gone).
 * Rule 3: whatever the records say, a body inside solid blocks is rescued. A player without a record and with a clear position is left alone and given a record.
 */
public final class Rescue {
    private Rescue() {}

    public static void onLogin(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !WeatherConfig.isQualifying(level)) return;
        WeatherData data = WeatherData.get(level.getServer());
        ChunkRecords rec = data.records;
        ChunkPos cp = p.chunkPosition();
        String dim = Records.dim(level);
        boolean reset = rec.resetAfterLogout(dim, cp.pack(), p.getUUID());
        try {
            level.getChunk(cp.x(), cp.z(), ChunkStatus.FULL, true);
        } catch (Throwable t) {
            WeatherOfArrakis.LOGGER.warn("Rescue could not load the chunk of {}: {}", p.getGameProfile().name(), t.toString());
            return;
        }
        boolean buried = bodyInSolid(level, p);
        if (reset || buried) {
            rescue(level, p, reset ? "chunk was reset" : "body inside solid blocks");
        } else if (!rec.players.containsKey(p.getUUID())) {
            rec.players.put(p.getUUID(), rec.counter);
            data.setDirty();
        }
    }

    private static boolean bodyInSolid(ServerLevel level, ServerPlayer p) {
        BlockPos feet = p.blockPosition();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        return level.getBlockState(feet).isCollisionShapeFullBlock(level, feet) || level.getBlockState(head).isCollisionShapeFullBlock(level, head);
    }

    private static void rescue(ServerLevel level, ServerPlayer p, String why) {
        int x = p.blockPosition().getX(), z = p.blockPosition().getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.teleportTo(x + 0.5, y, z + 0.5);
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable("islandcraft_weatherofarrakis.rescue").withStyle(ChatFormatting.GOLD));
        WeatherOfArrakis.LOGGER.info("Rescued {} to the surface at {} {} {} ({})", p.getGameProfile().name(), x, y, z, why);
    }
}
