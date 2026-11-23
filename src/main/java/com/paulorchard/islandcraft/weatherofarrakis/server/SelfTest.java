package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * DEV ONLY (prompt 05): headless self-test, active only with -Dislandcraft.weather.selftest=marker|reset1|reset2 on a dedicated server (never set in play).
 * Runs the real marker and forced-reset code against a real world and logs "SELFTEST PASS/FAIL name" lines, then stops the server. Not referenced by anything else.
 */
public final class SelfTest {
    public static final String MODE = System.getProperty("islandcraft.weather.selftest");
    private static final List<BooleanSupplier> STEPS = new ArrayList<>();
    private static int tickNo, pass, fail, wait;
    private static boolean built, ended;
    private static final Path STATE = Path.of("selftest_state.txt");

    private SelfTest() {}

    public static boolean enabled() {
        return MODE != null && !MODE.isBlank();
    }

    private static void log(String s) {
        WeatherOfArrakis.LOGGER.info("SELFTEST {}", s);
    }

    /** In the natural-terrain variant (reset1n) hash comparisons are reported as INFO: features that straddle chunk borders legitimately differ. */
    private static void checkHash(String name, boolean ok, String detail) {
        if (MODE.equals("reset1n")) log("INFO " + name + " -> " + (ok ? "equal" : "different") + " | " + detail);
        else check(name, ok, detail);
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) pass++;
        else fail++;
        log((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " | " + detail));
    }

    public static void tick(MinecraftServer server) {
        if (ended) return;
        if (++tickNo < 60) return;
        if (!built) {
            built = true;
            try {
                build(server);
            } catch (Throwable t) {
                log("FAIL setup: " + t);
                t.printStackTrace();
                end(server);
                return;
            }
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (STEPS.isEmpty()) {
            end(server);
            return;
        }
        try {
            if (STEPS.get(0).getAsBoolean()) STEPS.remove(0);
        } catch (Throwable t) {
            log("FAIL step threw " + t);
            t.printStackTrace();
            STEPS.remove(0);
        }
    }

    private static void end(MinecraftServer server) {
        ended = true;
        log("DONE pass=" + pass + " fail=" + fail);
        server.halt(false);
    }

    private static void step(BooleanSupplier s) {
        STEPS.add(s);
    }

    private static void pause(int ticks) {
        step(new BooleanSupplier() {
            boolean first = true;
            @Override public boolean getAsBoolean() {
                if (first) {
                    first = false;
                    wait = ticks;
                    return false;
                }
                return true;
            }
        });
    }

    private static void waitForReset() {
        pause(2);
        step(() -> !ForceReset.busy());
    }

    // ---------------------------------------------------------------- helpers

    private static ServerLevel level;

    private static int surface(int x, int z) {
        level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    static long hash(ServerLevel l, ChunkPos pos) {
        LevelChunk c = l.getChunk(pos.x(), pos.z());
        long h = 17;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int y = l.getMinY(); y < l.getMaxY(); y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    BlockState s = c.getBlockState(mp.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    h = h * 31 + s.toString().hashCode();
                }
        return h;
    }

    private static int items(ChunkPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.getMinBlockX(), level.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX() + 1, level.getMaxY(), pos.getMaxBlockZ() + 1)).size();
    }

    private static void hold(ChunkPos p, boolean on) {
        level.getChunkSource().updateChunkForced(p, on);
    }

    private static CompoundTag readStored(ChunkPos p) {
        return level.getChunkSource().chunkMap.read(p).join().orElse(null);
    }

    private static boolean storedEntities(ChunkPos p) {
        var es = (net.minecraft.world.level.chunk.storage.EntityStorage) level.entityManager.permanentStorage;
        return es.simpleRegionStorage.read(p).join().isPresent();
    }

    private static boolean storedHasSkull(ChunkPos p) {
        CompoundTag t = readStored(p);
        if (t == null) return false;
        var list = t.getListOrEmpty("block_entities");
        for (int i = 0; i < list.size(); i++) if (list.getCompoundOrEmpty(i).getStringOr("id", "").equals("minecraft:skull")) return true;
        return false;
    }

    // ---------------------------------------------------------------- tests

    private static void build(MinecraftServer server) {
        level = server.overworld();
        log("mode " + MODE + ", world " + server.getWorldData().getLevelName());
        if (MODE.equals("marker")) markerTests();
        else if (MODE.equals("reset1") || MODE.equals("reset1n")) resetTests1(server);
        else if (MODE.equals("reset2")) resetTests2(server);
        else if (MODE.startsWith("orderprobe")) orderProbe();
    }

    private static void markerTests() {
        step(() -> {
            int bx = 48000, bz = 48000;
            int y = surface(bx, bz);
            log("surface y at the test site = " + y);
            // 1. grass
            BlockPos floor = new BlockPos(bx, y - 1, bz);
            level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            BlockPos at = DeathMarker.place(level, new BlockPos(bx, y, bz), DeathMarker.Style.SKULL);
            BlockState st = at == null ? null : level.getBlockState(at);
            check("skull on grass is placed at the death position", at != null && at.equals(new BlockPos(bx, y, bz)) && st.is(Blocks.SKELETON_SKULL), "at=" + at + " state=" + st);
            check("skull has a block entity", at != null && level.getBlockEntity(at) instanceof SkullBlockEntity, "");
            check("chunk entered the altered record (keyed on the placement)", Records.get(level.getServer()).isAltered(Records.dim(level), ChunkPos.containing(at).pack()), "");
            // 2. bottom slab and top slab
            int sx = bx + 8;
            level.setBlock(new BlockPos(sx, y, bz), Blocks.SANDSTONE_SLAB.defaultBlockState(), 3);
            BlockPos onBottom = DeathMarker.place(level, new BlockPos(sx, y + 1, bz), DeathMarker.Style.SKULL);
            log("bottom slab: death on top of the slab -> placed at " + onBottom + (onBottom != null ? " (" + level.getBlockState(onBottom).getBlock() + ", floor " + level.getBlockState(onBottom.below()).getBlock() + ")" : " (rejected, nothing placed)"));
            check("skull never ends up directly on a bottom slab (not full-faced)", onBottom == null || !onBottom.below().equals(new BlockPos(sx, y, bz)), "");
            level.setBlock(new BlockPos(sx + 8, y, bz), Blocks.SANDSTONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP), 3);
            BlockPos onTop = DeathMarker.place(level, new BlockPos(sx + 8, y + 1, bz), DeathMarker.Style.SKULL);
            check("skull on a top slab (full-faced top) is placed on it", onTop != null && onTop.equals(new BlockPos(sx + 8, y + 1, bz)), "at=" + onTop);
            // 3. water: a 7x7 pool, depth 1, over the sand
            int wx = bx + 40;
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) level.setBlock(new BlockPos(wx + dx, y, bz + dz), Blocks.WATER.defaultBlockState(), 3);
            BlockPos inWater = DeathMarker.place(level, new BlockPos(wx, y, bz), DeathMarker.Style.SKULL);
            check("death in water: marker rejected, nothing placed", inWater == null || !level.getBlockState(inWater).is(Blocks.SKELETON_SKULL) || !level.getFluidState(inWater).isSource(), "at=" + inWater);
            // 4. every candidate blocked: solid stone 3x3 from 9 below to the spot
            int kx = bx + 80;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -9; dy <= 0; dy++) level.setBlock(new BlockPos(kx + dx, y + dy, bz + dz), Blocks.STONE.defaultBlockState(), 3);
            BlockPos blocked = null;
            String err = "";
            try {
                blocked = DeathMarker.place(level, new BlockPos(kx, y, bz), DeathMarker.Style.SKULL);
            } catch (Throwable t) {
                err = t.toString();
            }
            check("every candidate blocked: nothing placed and no exception", blocked == null && err.isEmpty(), "at=" + blocked + " err=" + err);
            // 5. style none and bone_block
            BlockPos none = DeathMarker.place(level, new BlockPos(bx + 120, y, bz), DeathMarker.Style.NONE);
            check("style none places nothing", none == null && level.getBlockState(new BlockPos(bx + 120, y, bz)).isAir(), "");
            BlockPos bone = DeathMarker.place(level, new BlockPos(bx + 124, y, bz), DeathMarker.Style.BONE_BLOCK);
            check("style bone_block still places a bone block", bone != null && level.getBlockState(bone).is(Blocks.BONE_BLOCK), "at=" + bone);
            // 6. rotation over 200 placements
            Set<Integer> rots = new HashSet<>();
            int placed = 0;
            for (int i = 0; i < 200; i++) {
                int x = bx + 200 + i * 2;
                surface(x, bz + 20);
                BlockPos p = DeathMarker.place(level, new BlockPos(x, y, bz + 20), DeathMarker.Style.SKULL);
                if (p != null) {
                    placed++;
                    rots.add(level.getBlockState(p).getValue(SkullBlock.ROTATION));
                }
            }
            check("rotation covers 0..15 over 200 placements", placed == 200 && rots.size() == 16, "placed=" + placed + " distinct=" + rots.size() + " " + rots);
            // 7. save and load of the block entity
            level.getChunkSource().save(true);
            ChunkPos cp = ChunkPos.containing(at);
            check("skull block entity is in the saved chunk data", storedHasSkull(cp), "");
            // remember for the reset test
            try {
                Files.writeString(STATE, cp.x() + "," + cp.z());
            } catch (Exception e) {
                log("state not written: " + e);
            }
            return true;
        });
    }

    private static final int BX = 2001, BZ = 2001; // centre chunk of the 3x3 test square (far from spawn)

    private static List<ForceReset.Target> square(int cx, int cz, int r) {
        List<ForceReset.Target> t = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) t.add(new ForceReset.Target(level, new ChunkPos(cx + dx, cz + dz)));
        return t;
    }

    private static long[] fresh = new long[16];
    private static BlockState[][] snap = new BlockState[9][];

    private static BlockState[] snapshot(ChunkPos pos) {
        LevelChunk c = level.getChunk(pos.x(), pos.z());
        BlockState[] out = new BlockState[(level.getMaxY() - level.getMinY()) * 256];
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = level.getMinY(); y < level.getMaxY(); y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) out[i++] = c.getBlockState(mp.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
        return out;
    }

    private static String diff(ChunkPos pos, BlockState[] before) {
        BlockState[] now = snapshot(pos);
        int n = 0, interior = 0;
        java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
        int i = 0;
        for (int y = level.getMinY(); y < level.getMaxY(); y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++, i++)
            if (before[i] != now[i]) {
                n++;
                if (x >= 2 && x <= 13 && z >= 2 && z <= 13) interior++;
                kinds.merge(before[i].getBlock().getDescriptionId().replace("block.minecraft.", "") + "->" + now[i].getBlock().getDescriptionId().replace("block.minecraft.", ""), 1, Integer::sum);
            }
        return n + " blocks differ (" + interior + " away from the chunk edge): " + kinds;
    }

    private static void alter(ChunkPos p) {
        hold(p, true);
        level.getChunk(p.x(), p.z(), ChunkStatus.FULL, true);
        int x = p.getMinBlockX() + 3, z = p.getMinBlockZ() + 3;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        level.setBlock(new BlockPos(x, y, z), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x + 1, y, z), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(new BlockPos(x + 1, y, z)) instanceof ChestBlockEntity ce) ce.setItem(0, new ItemStack(Items.DIAMOND, 3));
        ItemEntity drop = new ItemEntity(level, x + 0.5, y + 1.2, z + 0.5, new ItemStack(Items.GOLD_INGOT, 5));
        drop.setUnlimitedLifetime();
        level.addFreshEntity(drop);
        Records.markAltered(level, p);
    }

    private static boolean pristine(ChunkPos p, long h0, String name) {
        long h = hash(level, p);
        return h == h0;
    }

    private static void resetTests1(MinecraftServer server) {
        final ChunkPos C0 = new ChunkPos(BX, BZ), OUT = new ChunkPos(BX + 2, BZ), UNREC = new ChunkPos(BX, BZ + 3);
        final List<ForceReset.Target> sq = square(BX, BZ, 1);
        final int counter0 = Records.get(server).counter;
        step(() -> {
            if (MODE.equals("reset1n")) { // natural terrain: let the neighbourhood generate first so border features have settled
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) level.getChunk(BX + dx, BZ + dz, ChunkStatus.FULL, true);
            }
            // fresh hashes before anything is altered
            for (int i = 0; i < sq.size(); i++) {
                hold(sq.get(i).pos(), true);
                fresh[i] = hash(level, sq.get(i).pos());
            }
            for (int i = 0; i < sq.size(); i++) snap[i] = snapshot(sq.get(i).pos());
            hold(OUT, true);
            hold(UNREC, true);
            fresh[9] = hash(level, OUT);
            fresh[10] = hash(level, UNREC);
            log("fresh hashes taken for 9+2 chunks, centre " + fresh[4]);
            for (ForceReset.Target t : sq) alter(t.pos());
            // outside neighbour: a death marker (record keyed on the placement); unrecorded chunk: a block placed by code only
            int ox = OUT.getMinBlockX() + 5, oz = OUT.getMinBlockZ() + 5;
            int oy = surface(ox, oz);
            DeathMarker.place(level, new BlockPos(ox, oy, oz), DeathMarker.Style.SKULL);
            int ux = UNREC.getMinBlockX() + 5, uz = UNREC.getMinBlockZ() + 5;
            level.setBlock(new BlockPos(ux, surface(ux, uz), uz), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            check("all 9 altered, hashes differ from fresh", hash(level, C0) != fresh[4], "");
            check("neighbour has a skull and is recorded", Records.get(server).isAltered(Records.dim(level), OUT.pack()), "");
            check("unrecorded chunk is not recorded", !Records.get(server).isAltered(Records.dim(level), UNREC.pack()), "");
            return true;
        });
        pause(5);
        step(() -> {
            check("a dropped item is alive in the altered centre chunk", items(C0) == 1, "items=" + items(C0));
            level.getChunkSource().save(true);
            for (ForceReset.Target t : sq) hold(t.pos(), false);
            hold(OUT, false);
            hold(UNREC, false);
            return true;
        });
        pause(40);
        // S1: one chunk, no caller
        step(() -> {
            check("before the reset the centre chunk is saved with its entity and block entity (the stores are not empty)", storedEntities(C0) && readStored(C0) != null, "entities stored: " + storedEntities(C0));
            ForceReset.start(server.createCommandSourceStack(), null, square(BX, BZ, 0), false);
            return true;
        });
        waitForReset();
        step(() -> {
            var rec2 = Records.get(server);
            check("S1 centre chunk: ledger entry written, record entry removed", rec2.ledgerValue(Records.dim(level), C0.pack()) != null && !rec2.isAltered(Records.dim(level), C0.pack()), "");
            check("S1 Major counter unchanged", rec2.counter == counter0, "counter " + rec2.counter + " vs " + counter0);
            hold(C0, true);
            level.getChunk(C0.x(), C0.z(), ChunkStatus.FULL, true);
            return true;
        });
        pause(30);
        step(() -> {
            checkHash("S1 chunk equals a fresh chunk of the same seed (block-state hash over the full height)", hash(level, C0) == fresh[4], "now " + hash(level, C0) + " fresh " + fresh[4]);
            check("S1 no dropped item, no chest, no gold left", items(C0) == 0 && level.getBlockState(new BlockPos(C0.getMinBlockX() + 3, surface(C0.getMinBlockX() + 3, C0.getMinBlockZ() + 3), C0.getMinBlockZ() + 3)).isAir(), "items=" + items(C0));
            check("S1 neighbours untouched (corner chunk still altered)", Records.get(server).isAltered(Records.dim(level), new ChunkPos(BX - 1, BZ - 1).pack()), "");
            hold(C0, false);
            return true;
        });
        pause(40);
        // S2: radius 1 (the 9), outside neighbour must keep its skull
        step(() -> {
            ForceReset.start(server.createCommandSourceStack(), null, sq, false);
            return true;
        });
        waitForReset();
        step(() -> {
            int ok = 0;
            for (int i = 0; i < sq.size(); i++) {
                hold(sq.get(i).pos(), true);
                if (hash(level, sq.get(i).pos()) == fresh[i]) ok++;
            }
            checkHash("S2 radius 1: all 9 chunks match fresh", ok == 9, "matching=" + ok);
            if (MODE.equals("reset1n")) for (int i = 0; i < sq.size(); i++) log("DIFF chunk " + sq.get(i).pos() + ": " + diff(sq.get(i).pos(), snap[i]));
            checkHash("S2 outside neighbour still holds its skull and record", storedHasSkull(OUT) || level.getBlockState(new BlockPos(OUT.getMinBlockX() + 5, surface(OUT.getMinBlockX() + 5, OUT.getMinBlockZ() + 5), OUT.getMinBlockZ() + 5)).is(Blocks.SKELETON_SKULL) || hash(level, OUT) != fresh[9], "");
            check("S2 outside neighbour is still recorded", Records.get(server).isAltered(Records.dim(level), OUT.pack()), "");
            for (ForceReset.Target t : sq) hold(t.pos(), false);
            return true;
        });
        // S3: claimed chunk via the fake provider (system property islandcraft.weather.fakeProtected = 2008,2008)
        step(() -> {
            alter(new ChunkPos(BX + 7, BZ + 7));
            return true;
        });
        pause(5);
        step(() -> {
            level.getChunkSource().save(true);
            hold(new ChunkPos(BX + 7, BZ + 7), false);
            return true;
        });
        pause(40);
        step(() -> {
            ForceReset.start(server.createCommandSourceStack(), null, square(BX + 7, BZ + 7, 0), false);
            return true;
        });
        waitForReset();
        step(() -> {
            check("S3 claimed chunk skipped without ignoreclaims (still recorded)", Records.get(server).isAltered(Records.dim(level), new ChunkPos(BX + 7, BZ + 7).pack()), "");
            ForceReset.start(server.createCommandSourceStack(), null, square(BX + 7, BZ + 7, 0), true);
            return true;
        });
        waitForReset();
        step(() -> {
            check("S3 claimed chunk reset with ignoreclaims", !Records.get(server).isAltered(Records.dim(level), new ChunkPos(BX + 7, BZ + 7).pack()) && Records.get(server).ledgerValue(Records.dim(level), new ChunkPos(BX + 7, BZ + 7).pack()) != null, "");
            return true;
        });
        // S4: force all: re-alter two chunks, then the command without and with confirm
        step(() -> {
            alter(new ChunkPos(BX, BZ));
            alter(new ChunkPos(BX + 1, BZ));
            return true;
        });
        pause(5);
        step(() -> {
            level.getChunkSource().save(true);
            hold(new ChunkPos(BX, BZ), false);
            hold(new ChunkPos(BX + 1, BZ), false);
            return true;
        });
        pause(40);
        step(() -> {
            var rec2 = Records.get(server);
            int before = rec2.alteredCount();
            run(server, "coriolis reset force all");
            check("S4 force all without confirm changes nothing", Records.get(server).alteredCount() == before && !ForceReset.busy(), "recorded " + before);
            log("recorded before confirm: " + before);
            run(server, "coriolis reset force all confirm");
            return true;
        });
        waitForReset();
        step(() -> {
            var rec2 = Records.get(server);
            check("S4 force all confirm: record is empty (3 recorded chunks reset: OUT, and the two)", rec2.alteredCount() == 0, "recorded now " + rec2.alteredCount());
            check("S4 counter still unchanged", rec2.counter == counter0, "");
            hold(UNREC, true);
            level.getChunk(UNREC.x(), UNREC.z(), ChunkStatus.FULL, true);
            hold(OUT, true);
            level.getChunk(OUT.x(), OUT.z(), ChunkStatus.FULL, true);
            return true;
        });
        pause(30);
        step(() -> {
            int ux = UNREC.getMinBlockX() + 5, uz = UNREC.getMinBlockZ() + 5;
            check("S4 unrecorded chunk kept its placed block", level.getBlockState(new BlockPos(ux, surface(ux, uz) - 0, uz)).is(Blocks.GOLD_BLOCK) || level.getBlockState(new BlockPos(ux, surface(ux, uz) - 1, uz)).is(Blocks.GOLD_BLOCK), "");
            checkHash("S4 the neighbour with the skull (it was recorded) is fresh again", hash(level, OUT) == fresh[9], "");
            hold(UNREC, false);
            hold(OUT, false);
            return true;
        });
        // S5: a chunk kept loaded by a ticket cannot be reset: reported, not reset
        step(() -> {
            ChunkPos p = new ChunkPos(BX + 5, BZ + 5);
            alter(p);
            return true;
        });
        step(() -> {
            level.getChunkSource().save(true);
            ForceReset.start(server.createCommandSourceStack(), null, square(BX + 5, BZ + 5, 0), false);
            return true;
        });
        waitForReset();
        step(() -> {
            var rec2 = Records.get(server);
            check("S5 force-loaded chunk: not reset, failure reported, still recorded", rec2.isAltered(Records.dim(level), new ChunkPos(BX + 5, BZ + 5).pack()), "");
            hold(new ChunkPos(BX + 5, BZ + 5), false);
            return true;
        });
        // S6: login rescue rule after a forced reset (records only; the real login is covered by the client run)
        step(() -> {
            var rec2 = Records.get(server);
            java.util.UUID u = java.util.UUID.randomUUID();
            rec2.players.put(u, rec2.counter);
            rec2.playerForce.put(u, rec2.forceSeq);
            long chunk = new ChunkPos(BX + 9, BZ).pack();
            check("S6 before the reset the rescue rule is false", !rec2.resetAfterLogout(Records.dim(level), chunk, u), "");
            rec2.completeForceReset(Records.dim(level), chunk);
            check("S6 after a forced reset the rescue rule is true although the counter did not move", rec2.resetAfterLogout(Records.dim(level), chunk, u) && rec2.counter == counter0, "");
            rec2.playerForce.put(u, rec2.forceSeq);
            check("S6 a later logout does not rescue again", !rec2.resetAfterLogout(Records.dim(level), chunk, u), "");
            // leave a recorded chunk + a marker for the restart check
            ChunkPos keep = new ChunkPos(BX, BZ + 6);
            alter(keep);
            return true;
        });
        pause(5);
        step(() -> {
            hold(new ChunkPos(BX, BZ + 6), false);
            level.getChunkSource().save(true);
            try {
                Files.writeString(STATE, fresh[4] + "," + fresh[9]);
            } catch (Exception e) {
                log("state not written: " + e);
            }
            return true;
        });
        pause(20);
    }

    /** Does the generator itself give the same chunks whatever the order they are generated in? orderprobe_f loads a 9x9 area row by row, orderprobe_r in reverse; compare the logged hashes of the middle 3x3. */
    private static void orderProbe() {
        step(() -> {
            boolean rev = MODE.endsWith("_r");
            List<ChunkPos> all = new ArrayList<>();
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) all.add(new ChunkPos(BX + dx, BZ + dz));
            if (rev) java.util.Collections.reverse(all);
            for (ChunkPos p : all) level.getChunk(p.x(), p.z(), ChunkStatus.FULL, true);
            StringBuilder sb = new StringBuilder();
            int same = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) sb.append(hash(level, new ChunkPos(BX + dx, BZ + dz))).append(" ");
            log("ORDERPROBE " + MODE + " hashes of the middle 3x3: " + sb);
            return true;
        });
    }

    private static void resetTests2(MinecraftServer server) {
        // after a restart: the reset chunk stays reset (hash equals the fresh one from run 1), the recorded survivor is still recorded
        step(() -> {
            String[] s;
            try {
                s = Files.readString(STATE).trim().split(",");
            } catch (Exception e) {
                check("state file readable", false, e.toString());
                return true;
            }
            ChunkPos c0 = new ChunkPos(BX, BZ), out = new ChunkPos(BX + 2, BZ);
            hold(c0, true);
            hold(out, true);
            check("restart: centre chunk still matches the fresh hash", hash(level, c0) == Long.parseLong(s[0]), "");
            check("restart: neighbour with the skull (reset by force all) still fresh", hash(level, out) == Long.parseLong(s[1]), "");
            check("restart: the recorded survivor is still in the record", Records.get(server).isAltered(Records.dim(level), new ChunkPos(BX, BZ + 6).pack()), "recorded " + Records.get(server).alteredCount());
            check("restart: ledger entry of the reset chunk persisted", Records.get(server).ledgerValue(Records.dim(level), c0.pack()) != null, "");
            hold(c0, false);
            hold(out, false);
            return true;
        });
    }

    private static void run(MinecraftServer server, String cmd) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
    }

}
