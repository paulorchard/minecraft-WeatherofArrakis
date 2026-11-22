package com.paulorchard.islandcraft.weatherofarrakis.claims;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** A source of chunk protection (a claim mod, or later Subfiefs of Arrakis). The registry fails closed. */
public interface ChunkProtection {
    /** Short stable id, used in the claim_providers config list (for example "ftbchunks"). */
    String id();

    /** True if the chunk is claimed or protected. May throw; the registry then treats the chunk as protected. */
    boolean isProtected(ServerLevel level, ChunkPos pos) throws Exception;

    /** Short human text for coriolis chunks, for example "FTB Chunks: team Foo". Only called when isProtected is true. */
    String describe(ServerLevel level, ChunkPos pos);
}
