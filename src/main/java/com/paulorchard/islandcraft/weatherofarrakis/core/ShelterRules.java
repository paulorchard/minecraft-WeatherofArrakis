package com.paulorchard.islandcraft.weatherofarrakis.core;

/**
 * Is a player sheltered from the Coriolis storm? No game types, so it is unit-tested.
 * Sheltered = sky light at the head is at most deepCover, or a roof is within roofBlocks above the head and, on both the feet and head rows,
 * a solid block lies upwind within windbreakBlocks (the search carries on up to windbreakUnderRoof blocks while a roof continues overhead).
 */
public final class ShelterRules {
    public interface Blocks {
        /** Full-cube collision block (leaves count; plants, fences, doors, slabs, stairs, torches, water do not). */
        boolean solid(int x, int y, int z);

        /** Sky light 0..15 at the block, or -1 when unknown (unloaded). */
        int skyLight(int x, int y, int z);
    }

    public record Settings(int deepCoverSkyLight, int roofBlocks, int windbreakBlocks, int windbreakUnderRoof) {
        public static final Settings DEFAULT = new Settings(2, 3, 2, 16);
    }

    private ShelterRules() {}

    /**
     * @param feetY y of the block the feet are in; the head is feetY + 1
     * @param windX,windZ the direction the wind blows towards (unit, e.g. 1,0 for a west wind); upwind is the opposite way
     */
    public static boolean isSheltered(Blocks w, Settings s, int x, int feetY, int z, int windX, int windZ) {
        int headY = feetY + 1;
        int sky = w.skyLight(x, headY, z);
        if (sky >= 0 && sky <= s.deepCoverSkyLight()) return true;
        if (!hasRoof(w, s, x, headY, z)) return false;
        return windbreak(w, s, x, feetY, headY, z, windX, windZ) && windbreak(w, s, x, headY, headY, z, windX, windZ);
    }

    private static boolean hasRoof(Blocks w, Settings s, int x, int headY, int z) {
        for (int dy = 1; dy <= s.roofBlocks(); dy++) if (w.solid(x, headY + dy, z)) return true;
        return false;
    }

    private static boolean windbreak(Blocks w, Settings s, int x, int row, int headY, int z, int windX, int windZ) {
        for (int d = 1; d <= s.windbreakUnderRoof(); d++) {
            int cx = x - windX * d, cz = z - windZ * d;
            if (d > s.windbreakBlocks() && !hasRoof(w, s, cx, headY, cz)) return false;
            if (w.solid(cx, row, cz)) return true;
        }
        return false;
    }
}
