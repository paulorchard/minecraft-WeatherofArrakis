package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The marker left where a player dies in the Coriolis storm (prompt 05; before that a bone block). Style from the config bone_marker_style:
 * skull (a skeleton skull on the floor, random rotation), bone_block (the old marker), none (nothing, still logged).
 * Spot rules are the ones of prompt 01: the death position, else down up to 8, else the eight neighbours at the same height;
 * the spot must be air without fluid and have a block below with a sturdy top face. (Vanilla skulls have no survival check of their own,
 * so a skull never pops off; the sturdy-top rule alone keeps it on a solid floor and rejects bottom slabs, stairs and the like.)
 */
public final class DeathMarker {
    public enum Style {
        SKULL, BONE_BLOCK, NONE;

        public static Style parse(String s) {
            if (s == null) return SKULL;
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "bone_block" -> BONE_BLOCK;
                case "none" -> NONE;
                default -> SKULL;
            };
        }
    }

    private DeathMarker() {}

    public static boolean free(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState s = level.getBlockState(pos);
        if (!s.isAir() || !s.getFluidState().isEmpty()) return false;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** The spot itself, else down up to 8, else the eight neighbours at the same height. Null when nothing fits. */
    public static BlockPos findSpot(ServerLevel level, BlockPos death) {
        if (free(level, death)) return death;
        for (int dy = 1; dy <= 8; dy++) if (free(level, death.below(dy))) return death.below(dy);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) if ((dx != 0 || dz != 0) && free(level, death.offset(dx, 0, dz))) return death.offset(dx, 0, dz);
        return null;
    }

    /** Places the configured marker for a death at the given position; returns where it went, or null. The chunk enters the altered record when something was placed. */
    public static BlockPos place(ServerLevel level, BlockPos death) {
        return place(level, death, Style.parse(WeatherConfig.BONE_MARKER_STYLE.get()));
    }

    public static BlockPos place(ServerLevel level, BlockPos death, Style style) {
        if (style == Style.NONE) {
            WeatherOfArrakis.LOGGER.info("Death marker: style none, nothing placed for a death at {}", death);
            return null;
        }
        BlockPos at = findSpot(level, death);
        if (at == null) {
            WeatherOfArrakis.LOGGER.info("Death marker: no free spot with a solid floor near {}, nothing placed", death);
            return null;
        }
        BlockState state = style == Style.SKULL
                ? Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, level.getRandom().nextInt(16))
                : Blocks.BONE_BLOCK.defaultBlockState();
        level.setBlock(at, state, 3);
        Records.onMarker(level, at);
        WeatherOfArrakis.LOGGER.info("Death marker: {} placed at {} (death at {})", style.name().toLowerCase(Locale.ROOT), at, death);
        return at;
    }
}
