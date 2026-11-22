package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.claims.ClaimProviders;
import com.paulorchard.islandcraft.weatherofarrakis.core.ChunkRecords;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;

/** Commands of Part 6: coriolis major, chunks, reset all/here, mark. */
final class MajorCommands {
    private MajorCommands() {}

    private static Component t(String s) {
        return Component.literal(s);
    }

    static int start(CommandSourceStack src, int minutes, int stormSeconds, int windowSeconds) {
        if (!MajorStorm.start(src.getServer(), minutes, stormSeconds, windowSeconds)) {
            src.sendFailure(t("A Major storm is already planned or running (" + MajorStorm.describe() + "), or no dimension is configured."));
            return 0;
        }
        src.sendSuccess(() -> t("Major Coriolis storm in " + minutes + " minutes. Warnings run in the last "
                + WeatherConfig.APPROACH_SECONDS.get() + " s. Host: restart the server about 15 minutes after it hits."), true);
        return 1;
    }

    static int cancel(CommandSourceStack src) {
        if (!MajorStorm.cancel(src.getServer())) {
            src.sendFailure(t("Nothing to cancel (no Major storm planned, or players are already being disconnected)."));
            return 0;
        }
        src.sendSuccess(() -> t("Major storm cancelled."), true);
        return 1;
    }

    private static int chunkInfo(CommandSourceStack src, ChunkPos pos) {
        ServerLevel level = src.getLevel();
        ChunkRecords rec = Records.get(src.getServer());
        String dim = Records.dim(level);
        Integer last = rec.lastVisit(dim, pos.pack());
        Integer reset = rec.ledgerValue(dim, pos.pack());
        Optional<String> prot = ClaimProviders.protectedBy(level, pos);
        boolean onDisk = level.getChunkSource().chunkMap.read(pos).join().isPresent();
        boolean loaded = ChunkReset.isLoaded(level, pos);
        String s = "Chunk " + pos.x() + "," + pos.z() + " in " + dim + ": altered " + (last != null) + (last != null ? " (last visit counter " + last + ", counter now " + rec.counter + ")" : "")
                + "; protected: " + prot.orElse("no") + "; last reset at counter " + (reset == null ? "never" : reset)
                + "; saved on disk: " + onDisk + "; loaded: " + loaded;
        src.sendSuccess(() -> t(s), false);
        return 1;
    }

    static int chunks(CommandSourceStack src, ChunkPos pos) {
        if (pos != null) return chunkInfo(src, pos);
        ChunkRecords rec = Records.get(src.getServer());
        int resetAfter = WeatherConfig.RESET_AFTER_MAJORS.get();
        int wouldReset = 0, protectedCount = 0;
        for (ChunkRecords.Candidate c : rec.candidates(resetAfter, true)) {
            ServerLevel l = levelOf(src, c.dimension());
            if (l != null && ClaimProviders.isProtected(l, ChunkPos.unpack(c.chunk()))) protectedCount++;
            else wouldReset++;
        }
        int w = wouldReset, p = protectedCount;
        String s = "Major counter " + rec.counter + "; chunks recorded " + rec.alteredCount() + "; would reset now " + w + " (eligible but protected: " + p + "); ledger " + rec.ledgerCount()
                + "; claim providers: " + ClaimProviders.activeIds();
        src.sendSuccess(() -> t(s), false);
        return 1;
    }

    private static ServerLevel levelOf(CommandSourceStack src, String dim) {
        for (ServerLevel l : src.getServer().getAllLevels()) if (Records.dim(l).equals(dim)) return l;
        return null;
    }

    static int resetAll(CommandSourceStack src) {
        ChunkRecords rec = Records.get(src.getServer());
        List<ChunkRecords.Candidate> all = rec.candidates(0, false);
        if (!MajorStorm.startManual(src.getServer(), src, new ChunkReset(src.getServer(), all))) {
            src.sendFailure(t("A reset is already running."));
            return 0;
        }
        src.sendSuccess(() -> t("Resetting " + all.size() + " recorded chunks now, ignoring age. Protected chunks are skipped, and so are chunks that are loaded (no in-place fallback); a summary follows."), true);
        return 1;
    }

    static int resetHere(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        ChunkPos pos = ChunkPos.containing(net.minecraft.core.BlockPos.containing(src.getPosition()));
        List<ChunkRecords.Candidate> one = new ArrayList<>();
        one.add(new ChunkRecords.Candidate(Records.dim(level), pos.pack(), 0));
        if (!MajorStorm.startManual(src.getServer(), src, new ChunkReset(src.getServer(), one))) {
            src.sendFailure(t("A reset is already running."));
            return 0;
        }
        src.sendSuccess(() -> t("Resetting chunk " + pos.x() + "," + pos.z() + " (skipped if protected or loaded; a chunk with a player in it is loaded)."), true);
        return 1;
    }

    static int mark(CommandSourceStack src, ChunkPos pos) {
        ChunkPos p = pos != null ? pos : ChunkPos.containing(net.minecraft.core.BlockPos.containing(src.getPosition()));
        Records.markAltered(src.getLevel(), p);
        src.sendSuccess(() -> t("Chunk " + p.x() + "," + p.z() + " recorded as altered."), true);
        return 1;
    }

    @SuppressWarnings("unused")
    private static final Object UNUSED = IOWorker.STORE_EMPTY;
}
