package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.claims.ClaimProviders;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Prompt 05: forced chunk reset (a testing tool). Ignores the altered record, the age rule and the "loaded by a player" rule; skips claimed chunks unless told not to.
 * The Major storm counter is not touched and nothing is announced.
 *
 * Mechanism (unload, delete, load fresh): chunks held loaded by the caller are released by moving the caller (only the caller) out of range, to a safe spot
 * view distance + 17 chunks from the area (a player holds chunks at full status to view distance + 2 and at lower status for about 12 chunks more; measured); once every target has unloaded, its saved region, entity and POI data is deleted through the game's IO workers
 * (same code as the Major storm reset), the deletion is verified by reading all three stores back, and the caller is put back at the same x and z on the new
 * surface. The chunks then regenerate from the generator when the caller (or anyone) loads them. Chunks kept loaded by anything else (another player,
 * /forceload, a chunk loader) are reported as failed with that reason; other players are never moved. There is no in-place regeneration.
 */
public final class ForceReset {
    private static final int UNLOAD_TIMEOUT_TICKS = 300;
    /**
     * A player keeps chunks loaded to view distance + 2 at full status, and holders at lower status (which still block unloading) for about 12 chunks more
     * (measured: with view distance 10 the target was still held at 15 chunks away, holders ended about 24 away).
     */
    private static final int HOLD_MARGIN = 15;
    private static final List<ForceReset> RUNNING = new ArrayList<>();

    private final MinecraftServer server;
    private final CommandSourceStack src;
    private final ServerPlayer caller;
    private final List<Target> todo = new ArrayList<>();
    private final Map<String, Integer> claimed = new LinkedHashMap<>();
    private final Map<String, Integer> failed = new LinkedHashMap<>();
    private final Set<String> playersInRange = new LinkedHashSet<>();
    private final int asked;
    private int reset;
    private int ticks;
    private int pending = -1;
    private final long startNanos = System.nanoTime();
    private String movedNote = "";
    private ServerLevel originLevel;
    private double ox, oz;
    private boolean moved;
    private boolean finished;

    record Target(ServerLevel level, ChunkPos pos) {}

    private ForceReset(CommandSourceStack src, ServerPlayer caller, int asked) {
        this.server = src.getServer();
        this.src = src;
        this.caller = caller;
        this.asked = asked;
    }

    public static boolean busy() {
        return !RUNNING.isEmpty();
    }

    public static void tickAll(MinecraftServer server) {
        if (RUNNING.isEmpty()) return;
        for (ForceReset r : new ArrayList<>(RUNNING)) r.tick();
        RUNNING.removeIf(r -> r.finished);
    }

    public static void abortAll() {
        for (ForceReset r : new ArrayList<>(RUNNING)) r.finish("server stopping");
        RUNNING.clear();
    }

    private static void bump(Map<String, Integer> m, String k) {
        m.merge(k, 1, Integer::sum);
    }

    public static int viewRange(MinecraftServer server) {
        return Math.max(server.getPlayerList().getViewDistance(), server.getPlayerList().getSimulationDistance());
    }

    /** Starts a forced reset of the given chunks. Returns false (after telling the source) when one is already running. */
    static boolean start(CommandSourceStack src, ServerPlayer caller, List<Target> all, boolean ignoreClaims) {
        if (busy()) {
            src.sendFailure(Component.literal("A forced reset is already running."));
            return false;
        }
        ForceReset r = new ForceReset(src, caller, all.size());
        RUNNING.add(r);
        r.plan(all, ignoreClaims);
        return true;
    }

    private void plan(List<Target> all, boolean ignoreClaims) {
        int range = viewRange(server) + HOLD_MARGIN;
        List<Target> candidates = new ArrayList<>();
        for (Target t : all) {
            if (!ignoreClaims) {
                Optional<String> prot = ClaimProviders.protectedBy(t.level, t.pos);
                if (prot.isPresent()) {
                    bump(claimed, prot.get());
                    continue;
                }
            }
            // another player close enough to keep the chunk loaded: it cannot unload, and other players are never moved
            String near = null;
            for (ServerPlayer p : t.level.players()) {
                if (p == caller) continue;
                ChunkPos pc = p.chunkPosition();
                if (Math.max(Math.abs(pc.x() - t.pos.x()), Math.abs(pc.z() - t.pos.z())) <= range) {
                    near = p.getGameProfile().name();
                    playersInRange.add(near);
                    break;
                }
            }
            if (near != null) {
                bump(failed, "player in range (" + near + ")");
                continue;
            }
            candidates.add(t);
        }
        if (caller != null && !candidates.isEmpty() && caller.level() instanceof ServerLevel cl) {
            ChunkPos cc = caller.chunkPosition();
            boolean inRange = false;
            for (Target t : candidates)
                if (t.level == cl && Math.max(Math.abs(cc.x() - t.pos.x()), Math.abs(cc.z() - t.pos.z())) <= range + 1) {
                    inRange = true;
                    break;
                }
            if (inRange) {
                BlockPos dest = safeSpot(cl, cc, candidates, range + 2);
                if (dest == null) {
                    for (int i = 0; i < candidates.size(); i++) bump(failed, "no safe spot to move the caller out of range");
                    candidates.clear();
                } else {
                    originLevel = cl;
                    ox = caller.getX();
                    oz = caller.getZ();
                    caller.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
                    caller.resetFallDistance();
                    cl.getChunkSource().move(caller); // the player tickets follow the player now, not when the client next sends a move
                    moved = true;
                    movedNote = " " + caller.getGameProfile().name() + " was moved out of range and back to the surface at the same x and z (the old height may be rock or air now).";
                }
            }
        }
        todo.addAll(candidates);
        if (todo.isEmpty()) finish(null);
    }

    /** A standing spot at least minDist chunks (Chebyshev) from every target, trying 8 directions and growing distances. */
    private static BlockPos safeSpot(ServerLevel level, ChunkPos from, List<Target> targets, int minDist) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int mult = 1; mult <= 16; mult *= 2) {
            for (int[] d : dirs) {
                int cx = from.x() + d[0] * (minDist + 2) * mult, cz = from.z() + d[1] * (minDist + 2) * mult;
                boolean far = true;
                for (Target t : targets)
                    if (t.level == level && Math.max(Math.abs(cx - t.pos.x()), Math.abs(cz - t.pos.z())) < minDist) {
                        far = false;
                        break;
                    }
                if (!far) continue;
                try {
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
                } catch (Throwable t) {
                    continue;
                }
                int x = cx * 16 + 8, z = cz * 16 + 8;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                if (y <= level.getMinY() + 1 || !level.getBlockState(below).getFluidState().isEmpty() || level.getBlockState(below).isAir()) continue;
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    private void tick() {
        if (finished || pending >= 0) return; // while deleting, the completion callbacks finish the run
        ticks++;
        List<Target> stillLoaded = new ArrayList<>();
        for (Target t : todo) if (ChunkReset.isLoaded(t.level, t.pos)) stillLoaded.add(t);
        if (!stillLoaded.isEmpty() && ticks < UNLOAD_TIMEOUT_TICKS) return;
        for (Target t : stillLoaded) {
            String why = t.level.getChunkSource().ticketStorage.getTicketDebugString(t.pos.pack(), true);
            bump(failed, "still loaded after " + ticks / 20 + " s (tickets: " + why + "; a ticket that is not a player's: /forceload, a chunk loader)");
            todo.remove(t);
        }
        pending = todo.size();
        if (pending == 0) {
            finish(null);
            return;
        }
        for (Target t : new ArrayList<>(todo)) deleteOne(t);
    }

    private void deleteOne(Target t) {
        EntityStorage entities = (EntityStorage) t.level.entityManager.permanentStorage;
        var poi = t.level.getChunkSource().chunkMap.poiManager;
        ChunkReset.deleteSavedData(t.level, t.pos).thenCompose(v -> {
            var a = t.level.getChunkSource().chunkMap.read(t.pos);
            var b = entities.simpleRegionStorage.read(t.pos);
            var c = poi.simpleRegionStorage.read(t.pos);
            return CompletableFuture.allOf(a, b, c).thenApply(x -> List.of(a.join(), b.join(), c.join()));
        }).whenComplete((res, err) -> server.execute(() -> {
            boolean clean = err == null && res != null;
            if (clean) for (Optional<?> op : res) if (op.isPresent()) clean = false;
            if (clean) {
                WeatherData data = WeatherData.get(server);
                data.records.completeForceReset(Records.dim(t.level), t.pos.pack());
                data.setDirty();
                reset++;
            } else {
                bump(failed, err != null ? "error: " + err : "data still present after deleting");
                WeatherOfArrakis.LOGGER.warn("Forced reset of chunk {} in {} could not be verified ({})", t.pos, Records.dim(t.level), err);
            }
            if (--pending == 0) finish(null);
        }));
    }

    private void finish(String abortReason) {
        if (finished) return;
        finished = true;
        if (abortReason != null) bump(failed, abortReason);
        if (moved && caller != null && caller.isAlive() && !caller.hasDisconnected()) {
            try {
                int cx = BlockPos.containing(ox, 0, oz).getX() >> 4;
                int cz = BlockPos.containing(ox, 0, oz).getZ() >> 4;
                originLevel.getChunk(cx, cz, ChunkStatus.FULL, true);
                int y = originLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(ox), (int) Math.floor(oz));
                caller.teleportTo(ox, y, oz);
                caller.resetFallDistance();
                originLevel.getChunkSource().move(caller);
            } catch (Throwable t) {
                WeatherOfArrakis.LOGGER.warn("Could not put {} back after the forced reset: {}", caller.getGameProfile().name(), t.toString());
                movedNote += " COULD NOT teleport back: " + t;
            }
        }
        double secs = (System.nanoTime() - startNanos) / 1e9;
        StringBuilder sb = new StringBuilder("Forced reset: asked " + asked + ", reset " + reset);
        int claimedN = claimed.values().stream().mapToInt(Integer::intValue).sum();
        sb.append(", skipped (claimed) ").append(claimedN);
        if (claimedN > 0) sb.append(" [").append(String.join("; ", claimed.keySet())).append("]");
        int failedN = failed.values().stream().mapToInt(Integer::intValue).sum();
        sb.append(", failed ").append(failedN);
        if (failedN > 0) {
            List<String> parts = new ArrayList<>();
            failed.forEach((k, v) -> parts.add(v + "x " + k));
            sb.append(" [").append(String.join("; ", parts)).append("]");
        }
        sb.append(String.format(Locale.ROOT, ", took %.1f s.", secs));
        if (!playersInRange.isEmpty()) sb.append(" ").append(playersInRange.size()).append(" other player(s) in range (not moved): ").append(String.join(", ", playersInRange)).append(".");
        sb.append(movedNote);
        if (reset > 0) sb.append(" The chunks regenerate when next loaded; the Major counter is unchanged.");
        String msg = sb.toString();
        WeatherOfArrakis.LOGGER.info(msg);
        src.sendSuccess(() -> Component.literal(msg), true);
    }
}
