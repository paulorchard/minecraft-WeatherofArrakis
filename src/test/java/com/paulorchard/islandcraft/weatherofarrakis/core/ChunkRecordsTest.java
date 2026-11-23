package com.paulorchard.islandcraft.weatherofarrakis.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChunkRecordsTest {
    private static final String D = "minecraft:overworld";

    @Test
    void untouchedLandAddsNothing() {
        ChunkRecords r = new ChunkRecords();
        assertFalse(r.visit(D, 5L));
        assertEquals(0, r.alteredCount());
    }

    @Test
    void ageingLeftAloneResetsAfterThreeStorms() {
        ChunkRecords r = new ChunkRecords();
        r.markAltered(D, 1L);
        r.counter++; // storm 1
        assertFalse(r.isOldEnough(D, 1L, 3));
        r.counter++; // storm 2
        assertFalse(r.isOldEnough(D, 1L, 3));
        r.counter++; // storm 3
        assertTrue(r.isOldEnough(D, 1L, 3));
    }

    @Test
    void revisitedAfterStormTwoSurvivesStormThree() {
        ChunkRecords r = new ChunkRecords();
        r.markAltered(D, 1L);
        r.counter += 2;
        r.visit(D, 1L);
        r.counter++;
        assertFalse(r.isOldEnough(D, 1L, 3));
    }

    @Test
    void resetMovesChunkToLedgerAndOldestComesFirst() {
        ChunkRecords r = new ChunkRecords();
        r.markAltered(D, 1L);
        r.counter++;
        r.markAltered(D, 2L);
        r.counter += 3;
        var c = r.candidates(3, true);
        assertEquals(2, c.size());
        assertEquals(1L, c.get(0).chunk());
        r.completeReset(D, 1L);
        assertFalse(r.isAltered(D, 1L));
        assertEquals(4, r.ledgerValue(D, 1L));
    }

    @Test
    void rescueRuleOneComparesLedgerWithLogoutCounter() {
        ChunkRecords r = new ChunkRecords();
        UUID u = UUID.randomUUID();
        r.players.put(u, 2);
        r.markAltered(D, 1L);
        r.counter = 3;
        r.completeReset(D, 1L);
        assertTrue(r.resetAfterLogout(D, 1L, u));
        r.players.put(u, 3);
        assertFalse(r.resetAfterLogout(D, 1L, u));
        assertFalse(r.resetAfterLogout(D, 99L, u));
    }

    @Test
    void forcedResetLeavesTheCounterAloneButStillRescues() {
        ChunkRecords r = new ChunkRecords();
        UUID u = UUID.randomUUID();
        r.counter = 2;
        r.players.put(u, 2);
        r.playerForce.put(u, r.forceSeq);
        r.markAltered(D, 1L);
        r.completeForceReset(D, 1L);
        assertEquals(2, r.counter);
        assertFalse(r.isAltered(D, 1L));
        assertEquals(2, r.ledgerValue(D, 1L));
        assertTrue(r.resetAfterLogout(D, 1L, u), "a player who logged out before the forced reset is rescued although the counter is the same");
        r.playerForce.put(u, r.forceSeq); // logs out again after the reset
        assertFalse(r.resetAfterLogout(D, 1L, u));
        r.completeForceReset(D, 1L);
        assertTrue(r.resetAfterLogout(D, 1L, u), "a second forced reset counts again");
    }

    @Test
    void forcedResetOfAnUnrecordedChunkIsStillWrittenToTheLedger() {
        ChunkRecords r = new ChunkRecords();
        r.completeForceReset(D, 7L);
        assertNotNull(r.ledgerValue(D, 7L));
        assertEquals(1L, r.forceLedgerValue(D, 7L));
    }
}
