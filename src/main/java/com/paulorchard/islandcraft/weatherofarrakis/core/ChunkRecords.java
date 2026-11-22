package com.paulorchard.islandcraft.weatherofarrakis.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records of the Major storm (no game types). Chunks are keyed per dimension id by a packed long.
 * altered: chunk -> Major counter at its last visit. ledger: chunk -> counter value at the reset. players: uuid -> counter at last logout.
 */
public final class ChunkRecords {
    public int counter;
    public final Map<String, Map<Long, Integer>> altered = new HashMap<>();
    public final Map<String, Map<Long, Integer>> ledger = new HashMap<>();
    public final Map<UUID, Integer> players = new HashMap<>();

    public record Candidate(String dimension, long chunk, int lastVisit) {}

    /** A placed or broken block (or a bone marker): the chunk enters the record, last visit = now. */
    public void markAltered(String dim, long chunk) {
        altered.computeIfAbsent(dim, k -> new HashMap<>()).put(chunk, counter);
    }

    /** A player is near: an altered chunk gets last visit = now. Returns true if the chunk is recorded. */
    public boolean visit(String dim, long chunk) {
        Map<Long, Integer> m = altered.get(dim);
        if (m == null || !m.containsKey(chunk)) return false;
        m.put(chunk, counter);
        return true;
    }

    public boolean isAltered(String dim, long chunk) {
        Map<Long, Integer> m = altered.get(dim);
        return m != null && m.containsKey(chunk);
    }

    public Integer lastVisit(String dim, long chunk) {
        Map<Long, Integer> m = altered.get(dim);
        return m == null ? null : m.get(chunk);
    }

    public boolean isOldEnough(String dim, long chunk, int resetAfter) {
        Integer v = lastVisit(dim, chunk);
        return v != null && counter - v >= resetAfter;
    }

    public int alteredCount() {
        return altered.values().stream().mapToInt(Map::size).sum();
    }

    public int ledgerCount() {
        return ledger.values().stream().mapToInt(Map::size).sum();
    }

    /** All recorded chunks, oldest last-visit first; {@code onlyOldEnough} applies the ageing rule. */
    public List<Candidate> candidates(int resetAfter, boolean onlyOldEnough) {
        List<Candidate> out = new ArrayList<>();
        altered.forEach((dim, m) -> m.forEach((chunk, last) -> {
            if (!onlyOldEnough || counter - last >= resetAfter) out.add(new Candidate(dim, chunk, last));
        }));
        out.sort((a, b) -> Integer.compare(a.lastVisit(), b.lastVisit()));
        return out;
    }

    /** The chunk was reset: it leaves the altered record and enters the ledger at the current counter. */
    public void completeReset(String dim, long chunk) {
        Map<Long, Integer> m = altered.get(dim);
        if (m != null) m.remove(chunk);
        ledger.computeIfAbsent(dim, k -> new HashMap<>()).put(chunk, counter);
    }

    public Integer ledgerValue(String dim, long chunk) {
        Map<Long, Integer> m = ledger.get(dim);
        return m == null ? null : m.get(chunk);
    }

    /** Rescue rule 1: the ledger says the chunk was reset in a storm later than the player's recorded value. */
    public boolean resetAfterLogout(String dim, long chunk, UUID player) {
        Integer l = ledgerValue(dim, chunk);
        Integer p = players.get(player);
        return l != null && p != null && l > p;
    }
}
