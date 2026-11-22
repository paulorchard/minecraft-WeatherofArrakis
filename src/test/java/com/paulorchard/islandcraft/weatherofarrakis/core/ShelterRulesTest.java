package com.paulorchard.islandcraft.weatherofarrakis.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Player at (0, feet 64, 0); wind blows towards +x (a west wind), so upwind is -x. */
class ShelterRulesTest {
    private static class W implements ShelterRules.Blocks {
        final Set<String> solid = new HashSet<>();
        int sky = 15;

        W set(int x, int y, int z) {
            solid.add(x + "," + y + "," + z);
            return this;
        }

        W wall(int x, int y0, int y1) {
            for (int y = y0; y <= y1; y++) set(x, y, 0);
            return this;
        }

        W roof(int y, int x0, int x1) {
            for (int x = x0; x <= x1; x++) set(x, y, 0);
            return this;
        }

        @Override public boolean solid(int x, int y, int z) { return solid.contains(x + "," + y + "," + z); }
        @Override public int skyLight(int x, int y, int z) { return sky; }
    }

    private static boolean sheltered(W w) {
        return ShelterRules.isSheltered(w, ShelterRules.Settings.DEFAULT, 0, 64, 0, 1, 0);
    }

    @Test void openSand() { assertFalse(sheltered(new W())); }
    @Test void cave() { W w = new W(); w.sky = 0; assertTrue(sheltered(w)); }
    @Test void closedRoom() { assertTrue(sheltered(new W().roof(66, -2, 2).wall(-2, 64, 65).wall(2, 64, 65))); }
    @Test void leeUnderOverhang() { assertTrue(sheltered(new W().roof(66, -1, 2).wall(-1, 64, 65))); }
    @Test void windwardSide() { assertFalse(sheltered(new W().roof(66, -1, 2).wall(1, 64, 65))); }
    @Test void floatingBlock() { assertFalse(sheltered(new W().set(0, 67, 0))); }
    @Test void westDoorway() { assertFalse(sheltered(new W().roof(66, -5, 2))); }
    @Test void eastDoorwayDeepRoom() { assertTrue(sheltered(new W().roof(66, -5, 2).wall(-5, 64, 65))); }
    @Test void roofFourUp() { assertFalse(sheltered(new W().roof(69, -3, 3).wall(-1, 64, 65))); }
    @Test void feetExposedThroughGap() { assertFalse(sheltered(new W().roof(66, -1, 2).wall(-1, 65, 65))); }
    @Test void unknownSkyLightIsNotDeepCover() {
        W w = new W() {
            @Override public int skyLight(int x, int y, int z) { return -1; }
        };
        assertFalse(sheltered(w));
    }
}
