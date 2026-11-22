package com.paulorchard.islandcraft.weatherofarrakis.claims;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** FTB Chunks via reflection: FTBChunksAPI.api().getOwningTeam(Level, ChunkPos) returns an Optional team (read from the 26.1.2.11 jar). */
final class FtbChunksProvider implements ChunkProtection {
    private final Object api;
    private final Method owningTeam;

    FtbChunksProvider() throws Exception {
        Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
        this.api = apiClass.getMethod("api").invoke(null);
        this.owningTeam = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API").getMethod("getOwningTeam", Level.class, ChunkPos.class);
    }

    @Override
    public String id() {
        return "ftbchunks";
    }

    @Override
    public boolean isProtected(ServerLevel level, ChunkPos pos) throws Exception {
        Object r = owningTeam.invoke(api, level, pos);
        return r instanceof Optional<?> o && o.isPresent();
    }

    @Override
    public String describe(ServerLevel level, ChunkPos pos) {
        try {
            Object team = ((Optional<?>) owningTeam.invoke(api, level, pos)).orElse(null);
            if (team == null) return "FTB Chunks";
            return "FTB Chunks: team " + team.getClass().getMethod("getShortName").invoke(team);
        } catch (Exception e) {
            return "FTB Chunks";
        }
    }
}
