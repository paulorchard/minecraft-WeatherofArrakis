package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.claims.ClaimProviders;
import com.paulorchard.islandcraft.weatherofarrakis.core.ChunkRecords;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

/**
 * The chunk reset. Mechanism: unload-and-delete. For a chunk nobody has loaded, its saved data in all three stores is deleted
 * (region = blocks, entities = mobs/items, poi = points of interest) through the same IO workers the game uses, so a pending save cannot
 * write it back; the next load regenerates it from the world seed. The result is verified by reading the chunk back from the store.
 * A chunk that is loaded (or forced, or holds a ticket) is skipped and stays recorded; there is no in-place regeneration fallback.
 */
public final class ChunkReset {
    public static final int MAX_IN_FLIGHT = 8;

    public static final class Stats {
        public int checked, reset, skippedProtected, skippedLoaded, skippedRecent, failed, leftOver;
        public long startNanos = System.nanoTime();
        public int counter;
    }

    private final MinecraftServer server;
    private final Deque<ChunkRecords.Candidate> queue = new ArrayDeque<>();
    private int inFlight;
    private final long deadlineNanos;
    public final Stats stats = new Stats();
    private boolean closed;
    private final boolean ignoreAge;

    /** @param windowSeconds time budget; 0 or less = unlimited (commands) */
    public ChunkReset(MinecraftServer server, int windowSeconds, boolean ignoreAge) {
        this.server = server;
        this.ignoreAge = ignoreAge;
        this.deadlineNanos = windowSeconds > 0 ? System.nanoTime() + windowSeconds * 1_000_000_000L : Long.MAX_VALUE;
        ChunkRecords rec = Records.get(server);
        stats.counter = rec.counter;
        queue.addAll(rec.candidates(WeatherConfig.RESET_AFTER_MAJORS.get(), !ignoreAge));
        stats.skippedRecent = ignoreAge ? 0 : rec.alteredCount() - queue.size();
    }

    public ChunkReset(MinecraftServer server, List<ChunkRecords.Candidate> only) {
        this.server = server;
        this.ignoreAge = true;
        this.deadlineNanos = Long.MAX_VALUE;
        stats.counter = Records.get(server).counter;
        queue.addAll(only);
    }

    public boolean isFinished() {
        return closed || (queue.isEmpty() && inFlight == 0);
    }

    /** Stops taking new chunks (window over or server stopping); chunks in hand finish. */
    public void stopIssuing() {
        stats.leftOver = queue.size();
        queue.clear();
    }

    /** Call once per server tick. */
    public void tick() {
        if (closed) return;
        if (System.nanoTime() > deadlineNanos && !queue.isEmpty()) stopIssuing();
        while (inFlight < MAX_IN_FLIGHT && !queue.isEmpty() && System.nanoTime() <= deadlineNanos) process(queue.poll());
        if (queue.isEmpty() && inFlight == 0) closed = true;
    }

    private ServerLevel level(String dim) {
        Identifier id = Identifier.tryParse(dim);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    /** Why a chunk cannot be reset right now, or empty if it can. */
    public static Optional<String> blocker(ServerLevel level, ChunkPos pos) {
        Optional<String> prot = ClaimProviders.protectedBy(level, pos);
        if (prot.isPresent()) return Optional.of("protected: " + prot.get());
        if (isLoaded(level, pos)) return Optional.of("loaded");
        return Optional.empty();
    }

    public static boolean isLoaded(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().chunkMap.getUpdatingChunkIfPresent(pos.pack()) != null
                || level.getChunkSource().getChunkNow(pos.x(), pos.z()) != null;
    }

    private void process(ChunkRecords.Candidate c) {
        stats.checked++;
        ServerLevel level = level(c.dimension());
        ChunkPos pos = ChunkPos.unpack(c.chunk());
        if (level == null) {
            stats.failed++;
            return;
        }
        if (ClaimProviders.isProtected(level, pos)) {
            stats.skippedProtected++;
            return;
        }
        if (isLoaded(level, pos)) {
            stats.skippedLoaded++;
            return;
        }
        inFlight++;
        deleteSavedData(level, pos).thenCompose(v -> level.getChunkSource().chunkMap.read(pos)).whenComplete((existing, err) -> server.execute(() -> {
            inFlight--;
            if (err != null || existing == null || existing.isPresent()) {
                stats.failed++;
                WeatherOfArrakis.LOGGER.warn("Reset of chunk {} in {} could not be verified ({})", pos, c.dimension(), err);
                return;
            }
            WeatherData data = WeatherData.get(server);
            data.records.completeReset(c.dimension(), c.chunk());
            data.setDirty();
            stats.reset++;
        }));
    }

    /**
     * Deletes region, entities and poi data of one chunk. Must run on the server thread. The poi manager keeps sections in memory,
     * so those are dropped as well, otherwise the next save would write them back.
     */
    public static CompletableFuture<Void> deleteSavedData(ServerLevel level, ChunkPos pos) {
        List<CompletableFuture<?>> all = new ArrayList<>();
        all.add(level.getChunkSource().chunkMap.write(pos, IOWorker.STORE_EMPTY));

        EntityStorage entities = (EntityStorage) level.entityManager.permanentStorage;
        all.add(entities.simpleRegionStorage.write(pos, IOWorker.STORE_EMPTY));

        PoiManager poi = level.getChunkSource().chunkMap.poiManager;
        SectionStorage<?, ?> sections = poi;
        int min = level.getMinSectionY(), max = level.getMaxSectionY();
        synchronized (sections) {
            for (int y = min; y <= max; y++) sections.storage.remove(net.minecraft.core.SectionPos.asLong(pos.x(), y, pos.z()));
            sections.dirtyChunks.remove(pos.pack());
            sections.loadedChunks.remove(pos.pack());
            sections.pendingLoads.remove(pos.pack());
        }
        all.add(sections.simpleRegionStorage.write(pos, IOWorker.STORE_EMPTY));
        return CompletableFuture.allOf(all.toArray(new CompletableFuture<?>[0]));
    }

    public String summary() {
        double secs = (System.nanoTime() - stats.startNanos) / 1e9;
        return String.format(java.util.Locale.ROOT, "Major pass: counter %d, checked %d, reset %d, skipped protected %d, skipped loaded %d, skipped too recent %d, failed %d, left over %d, took %.1f s",
                stats.counter, stats.checked, stats.reset, stats.skippedProtected, stats.skippedLoaded, stats.skippedRecent, stats.failed, stats.leftOver, secs);
    }

}
