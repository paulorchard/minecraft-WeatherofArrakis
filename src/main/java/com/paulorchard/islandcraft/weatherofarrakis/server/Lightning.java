package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import com.paulorchard.islandcraft.weatherofarrakis.net.ShakePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Part 4: lightning during STORM and camera shake requests. The bolt is the vanilla LightningBolt in visual-only mode: in the 26.1.2 source that
 * skips fire, lightning rods, copper, and thunderHit on entities (no pig to piglin, no villager to witch), while the client still gets
 * the flash and both thunder sounds. Damage to exposed players is ours.
 */
public final class Lightning {
    private static final Random RNG = new Random();
    private static final Map<String, Integer> COUNTDOWN = new HashMap<>();
    private static final double SHAKE_RANGE = 20.0;

    private Lightning() {}

    /** Called every tick for a qualifying level. */
    public static void tick(ServerLevel level) {
        if (!WeatherConfig.LIGHTNING_ENABLED.get() || WeatherManager.state(level).phase != Phase.STORM) return;
        String key = level.dimension().identifier().toString();
        int left = COUNTDOWN.getOrDefault(key, 40) - 1;
        if (left > 0) {
            COUNTDOWN.put(key, left);
            return;
        }
        double min = WeatherConfig.LIGHTNING_INTERVAL_MIN.get(), max = Math.max(min, WeatherConfig.LIGHTNING_INTERVAL_MAX.get());
        COUNTDOWN.put(key, (int) ((min + RNG.nextDouble() * (max - min)) * 20));
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (p.isAlive()) players.add(p);
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        int lo = WeatherConfig.LIGHTNING_MIN_DIST.get(), hi = Math.max(lo + 1, WeatherConfig.LIGHTNING_MAX_DIST.get());
        double angle = RNG.nextDouble() * Math.PI * 2, dist = lo + RNG.nextDouble() * (hi - lo);
        strike(level, target.getX() + Math.cos(angle) * dist, target.getZ() + Math.sin(angle) * dist);
    }

    /** One strike at x, z on the top block of that column. Works in any phase. */
    public static void strike(ServerLevel level, double x, double z) {
        BlockPos column = BlockPos.containing(x, 0, z);
        if (!level.hasChunkAt(column)) level.getChunk(column);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.snapTo(x, y, z);
        level.addFreshEntity(bolt);

        double radius = WeatherConfig.LIGHTNING_RADIUS.get();
        float damage = WeatherConfig.LIGHTNING_DAMAGE.get().floatValue();
        for (ServerPlayer p : level.players()) {
            double d = Math.sqrt(p.distanceToSqr(x, y, z));
            if (d <= SHAKE_RANGE) {
                float strength = (float) (0.6 * (1.0 - d / SHAKE_RANGE));
                PacketDistributor.sendToPlayer(p, new ShakePayload(strength, 0.6f));
            }
            if (radius > 0 && damage > 0 && d <= radius && p.isAlive() && !p.isCreative() && !p.isSpectator() && Exposure.isExposed(p)) {
                p.invulnerableTime = 0;
                p.hurtServer(level, Exposure.stormSource(level), damage);
            }
        }
    }

    /** Storm start: a strong shake of about 2 s for everyone, half strength if sheltered. */
    public static void stormStartShake(ServerLevel level) {
        if (!WeatherConfig.SHAKE_START.get()) return;
        for (ServerPlayer p : level.players()) {
            PacketDistributor.sendToPlayer(p, new ShakePayload(Exposure.isExposed(p) ? 1.0f : 0.5f, 2.0f));
        }
    }
}
