package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import com.paulorchard.islandcraft.weatherofarrakis.core.ShelterRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Part 5: shelter check, grace, gear loss, health loss and the death marker. Runs once a second per player while the phase is STORM. */
public final class Exposure {
    public static final ResourceKey<DamageType> STORM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(WeatherOfArrakis.MOD_ID, "arrakis_coriolis_storm"));
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private record Track(int exposedSeconds, int shown) {}

    /** Not saved. shown: 0 nothing, 1 sheltered, 2 warning, 3 damaging. */
    private static final Map<UUID, Track> TRACK = new HashMap<>();

    private Exposure() {}

    // ---- shelter ----

    private static final class LevelBlocks implements ShelterRules.Blocks {
        final ServerLevel level;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        LevelBlocks(ServerLevel level) {
            this.level = level;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) return false;
            return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
        }

        @Override
        public int skyLight(int x, int y, int z) {
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) return -1;
            return level.getBrightness(LightLayer.SKY, pos);
        }
    }

    public static ShelterRules.Settings settings() {
        return new ShelterRules.Settings(WeatherConfig.DEEP_COVER_SKY_LIGHT.get(), WeatherConfig.ROOF_BLOCKS.get(),
                WeatherConfig.WINDBREAK_BLOCKS.get(), WeatherConfig.WINDBREAK_UNDER_ROOF.get());
    }

    /** True when the player is outside any shelter right now (ignores phase and game mode). */
    public static boolean isExposed(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        float[] wind = WeatherConfig.windVector();
        BlockPos feet = player.blockPosition();
        return !ShelterRules.isSheltered(new LevelBlocks(level), settings(), feet.getX(), feet.getY(), feet.getZ(), Math.round(wind[0]), Math.round(wind[1]));
    }

    // ---- per-second step ----

    public static void tickSecond(ServerLevel level) {
        if (!WeatherConfig.EXPOSURE_ENABLED.get()) return;
        if (WeatherManager.state(level).phase != Phase.STORM) return;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) {
                TRACK.remove(p.getUUID());
                continue;
            }
            step(level, p);
        }
    }

    private static void step(ServerLevel level, ServerPlayer p) {
        Track t = TRACK.getOrDefault(p.getUUID(), new Track(0, 0));
        if (!isExposed(p)) {
            show(p, t, 1, "islandcraft_weatherofarrakis.exposure.sheltered", ChatFormatting.GREEN);
            return;
        }
        int secs = t.exposedSeconds() + 1;
        if (secs <= WeatherConfig.EXPOSURE_GRACE.get()) {
            show(p, new Track(secs, t.shown()), 2, "islandcraft_weatherofarrakis.exposure.warning", ChatFormatting.GOLD);
            return;
        }
        show(p, new Track(secs, t.shown()), 3, "islandcraft_weatherofarrakis.exposure.damage", ChatFormatting.RED);
        wearGear(level, p);
        float loss = (hasArmour(p) ? WeatherConfig.HEALTH_LOSS_ARMOURED.get() : WeatherConfig.HEALTH_LOSS_STRIPPED.get()).floatValue();
        if (loss > 0) {
            p.invulnerableTime = 0;
            p.hurtServer(level, stormSource(level), loss);
        }
    }

    /** Action-bar message, only when the state changes. Stores the new track. */
    private static void show(ServerPlayer p, Track t, int state, String key, ChatFormatting colour) {
        int secs = state == 1 ? 0 : t.exposedSeconds();
        if (t.shown() != state) p.sendSystemMessage(Component.translatable(key).withStyle(colour), true);
        TRACK.put(p.getUUID(), new Track(secs, state));
    }

    /** Tells one player at storm start whether they are sheltered. */
    public static void announce(ServerLevel level, ServerPlayer p) {
        if (!WeatherConfig.EXPOSURE_ENABLED.get() || p.isCreative() || p.isSpectator()) return;
        boolean exposed = isExposed(p);
        p.sendSystemMessage(Component.translatable(exposed ? "islandcraft_weatherofarrakis.exposure.start.exposed" : "islandcraft_weatherofarrakis.exposure.start.sheltered")
                .withStyle(exposed ? ChatFormatting.RED : ChatFormatting.GREEN), true);
        TRACK.put(p.getUUID(), new Track(0, exposed ? 2 : 1));
    }

    public static DamageSource stormSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STORM_DAMAGE));
    }

    private static boolean hasArmour(ServerPlayer p) {
        for (EquipmentSlot s : ARMOUR) {
            ItemStack st = p.getItemBySlot(s);
            if (!st.isEmpty() && st.isDamageableItem()) return true;
        }
        return false;
    }

    /** Armour and held item lose durability; an item at 0 is torn away (break sound, no drop). */
    private static void wearGear(ServerLevel level, ServerPlayer p) {
        int loss = WeatherConfig.DURABILITY_LOSS.get();
        if (loss <= 0) return;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
            ItemStack st = p.getItemBySlot(s);
            if (st.isEmpty() || !st.isDamageableItem()) continue;
            int dmg = st.getDamageValue() + loss;
            if (dmg >= st.getMaxDamage()) {
                p.setItemSlot(s, ItemStack.EMPTY);
                level.playSound(null, p.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            } else {
                st.setDamageValue(dmg);
            }
        }
    }

    // ---- death marker ----

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!WeatherConfig.isQualifying(level) || !WeatherConfig.EXPOSURE_ENABLED.get()) return;
        if (WeatherManager.state(level).phase != Phase.STORM) return;
        DeathMarker.place(level, p.blockPosition());
        TRACK.remove(p.getUUID());
    }

    public static void forget(UUID id) {
        TRACK.remove(id);
    }
}
