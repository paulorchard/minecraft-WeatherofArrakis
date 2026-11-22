package com.paulorchard.islandcraft.weatherofarrakis.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Chunk records of the Major storm (Part 6). Step 2 only receives the bone-marker hook. */
public final class Records {
    private Records() {}

    public static void onBoneMarker(ServerLevel level, BlockPos pos) {
        // Part 6: the chunk enters the altered record.
    }
}
