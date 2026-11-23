package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.claims.ClaimProviders;
import com.paulorchard.islandcraft.weatherofarrakis.core.ChunkRecords;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/** Prompt 05: coriolis reset force here/all, coriolis reset test here, coriolis help. */
final class ForceCommands {
    private ForceCommands() {}

    private static Component t(String s) {
        return Component.literal(s);
    }

    static LiteralArgumentBuilder<CommandSourceStack> forceNode() {
        return Commands.literal("force")
                .then(Commands.literal("here")
                        .executes(c -> here(c.getSource(), 0, false))
                        .then(Commands.literal("ignoreclaims").executes(c -> here(c.getSource(), 0, true)))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 4))
                                .executes(c -> here(c.getSource(), IntegerArgumentType.getInteger(c, "radius"), false))
                                .then(Commands.literal("ignoreclaims").executes(c -> here(c.getSource(), IntegerArgumentType.getInteger(c, "radius"), true)))))
                .then(Commands.literal("all")
                        .executes(c -> all(c.getSource(), false, false))
                        .then(Commands.literal("confirm").executes(c -> all(c.getSource(), false, true)))
                        .then(Commands.literal("ignoreclaims")
                                .executes(c -> all(c.getSource(), true, false))
                                .then(Commands.literal("confirm").executes(c -> all(c.getSource(), true, true)))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> testNode() {
        return Commands.literal("test").then(Commands.literal("here").executes(c -> testHere(c.getSource())));
    }

    static LiteralArgumentBuilder<CommandSourceStack> helpNode() {
        return Commands.literal("help").executes(c -> {
            String[] lines = {
                    "coriolis status | start [approach [storm]] | stop | strike [x z] | major <minutes> [storm [window]] | major cancel",
                    "coriolis chunks [x z] | mark [x z]   (the altered-chunk record)",
                    "coriolis reset all            - Major-style reset of every recorded chunk (ignores age; skips claimed and loaded chunks)",
                    "coriolis reset here           - the same for the chunk you stand in",
                    "coriolis reset force here [radius 0-4] [ignoreclaims] - TEST: reset your chunk and the square around it, ignoring the record, age and the loaded rule",
                    "coriolis reset force all [ignoreclaims] [confirm]     - TEST: reset every RECORDED chunk (never the whole world); without confirm it only counts",
                    "coriolis reset test here      - TEST: place a gold platform, a chest with an item and a dropped item here, and mark the chunk",
                    "sandstorm start [minutes] [thunder] | stop"};
            for (String l : lines) c.getSource().sendSuccess(() -> t(l), false);
            return 1;
        });
    }

    private static ServerPlayer player(CommandSourceStack src) {
        return src.getEntity() instanceof ServerPlayer p ? p : null;
    }

    private static int here(CommandSourceStack src, int radius, boolean ignoreClaims) {
        ServerPlayer p = player(src);
        if (p == null) {
            src.sendFailure(t("reset force here needs a player (it uses the chunk you stand in). From the console use reset force all."));
            return 0;
        }
        ServerLevel level = src.getLevel();
        ChunkPos c = p.chunkPosition();
        List<ForceReset.Target> targets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) targets.add(new ForceReset.Target(level, new ChunkPos(c.x() + dx, c.z() + dz)));
        if (!ForceReset.start(src, p, targets, ignoreClaims)) return 0;
        src.sendSuccess(() -> t("Forced reset of " + targets.size() + " chunk(s) around " + c.x() + "," + c.z() + " started: you will be moved out of range and back; a summary follows."), true);
        return 1;
    }

    private static List<ForceReset.Target> recorded(CommandSourceStack src) {
        ChunkRecords rec = Records.get(src.getServer());
        List<ForceReset.Target> out = new ArrayList<>();
        ServerPlayer p = player(src);
        for (ServerLevel l : src.getServer().getAllLevels()) {
            if (p != null ? l != src.getLevel() : !WeatherConfig.isQualifying(l)) continue;
            var m = rec.altered.get(Records.dim(l));
            if (m != null) for (Long chunk : m.keySet()) out.add(new ForceReset.Target(l, ChunkPos.unpack(chunk)));
        }
        return out;
    }

    private static int all(CommandSourceStack src, boolean ignoreClaims, boolean confirm) {
        List<ForceReset.Target> targets = recorded(src);
        int prot = 0;
        for (ForceReset.Target tg : targets) if (ClaimProviders.isProtected(tg.level(), tg.pos())) prot++;
        int p2 = prot;
        if (!confirm) {
            src.sendSuccess(() -> t("reset force all would reset " + (ignoreClaims ? targets.size() : targets.size() - p2) + " of " + targets.size() + " recorded chunk(s) ("
                    + (player(src) != null ? "this dimension" : "every configured dimension") + "); " + p2 + " are protected by a claim" + (ignoreClaims ? " (ignoreclaims: reset anyway)" : " (skipped)")
                    + ". Chunks that are not in the record are never touched. Add confirm to run it."), false);
            return 1;
        }
        if (targets.isEmpty()) {
            src.sendSuccess(() -> t("Nothing recorded; nothing reset."), false);
            return 1;
        }
        if (!ForceReset.start(src, player(src), targets, ignoreClaims)) return 0;
        src.sendSuccess(() -> t("Forced reset of " + targets.size() + " recorded chunk(s) started; a summary follows."), true);
        return 1;
    }

    /** A visible pattern in the caller's chunk: 3x3 gold platform in the middle, a chest with one item beside it, a dropped item; the chunk is marked altered. */
    private static int testHere(CommandSourceStack src) {
        ServerPlayer p = player(src);
        if (p == null) {
            src.sendFailure(t("reset test here needs a player."));
            return 0;
        }
        ServerLevel level = src.getLevel();
        ChunkPos c = p.chunkPosition();
        int mx = c.getMinBlockX() + 7, mz = c.getMinBlockZ() + 7;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mx + 1, mz + 1);
        for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) level.setBlock(new BlockPos(mx + dx, y, mz + dz), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        BlockPos chest = new BlockPos(mx + 3, y, mz + 1);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity ce) ce.setItem(0, new ItemStack(Items.DIAMOND, 3));
        ItemEntity drop = new ItemEntity(level, mx + 1.5, y + 1.2, mz + 1.5, new ItemStack(Items.GOLD_INGOT, 5));
        drop.setPickUpDelay(32767);
        drop.setUnlimitedLifetime();
        level.addFreshEntity(drop);
        Records.markAltered(level, c);
        src.sendSuccess(() -> t("Test pattern placed in chunk " + c.x() + "," + c.z() + " (gold platform at " + mx + " " + y + " " + mz + ", a chest with diamonds, a dropped gold ingot); the chunk is marked altered. Now run coriolis reset force here"), true);
        return 1;
    }
}
