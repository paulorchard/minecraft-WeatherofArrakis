// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.world;

import com.paulorchard.islandcraft.weatherofarrakis.storms.client.ImmersiveStormsClient;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.util.ISClientTickEvents;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISEnvironmentAttributes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.sound.StormSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

public final class SandstormSounds implements ISClientTickEvents.EndLevel {
    private static final int MAX_SOUND_Y_DIFF = 10;

    private static final int MAX_XZ_OFFSET = 10;

    private int timer = 0;
    /** IslandCraft change (prompt 04): number of gusts started, for the debug log. */
    public static int gustsPlayed;

    @Override
    public void onEndTick(ClientLevel level) {
        // IslandCraft change (prompt 04): the gate (rain 0.7) and the fixed gust timer below are replaced by the client settings wind_gust_*; the rate, volume, pitch and
        // sound follow the storm strength (StormInput) and the outdoor factor (StormWindBed). Original: if (level.getRainLevel(1f) < 0.7f || frozen) return;
        final float stormRain = com.paulorchard.islandcraft.weatherofarrakis.client.StormInput.rain(level, 1f);
        final float stormThunder = com.paulorchard.islandcraft.weatherofarrakis.client.StormInput.thunder(level, 1f);
        if (stormRain < 0.2f || level.tickRateManager().isFrozen() || !com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_GUST_ENABLED.get()) {
            return;
        }
        final float strength = Mth.clamp(0.7f * stormRain + 0.3f * stormThunder, 0f, 1f);

        if (!ImmersiveStormsClient.getConfig().isEnableStrongWindSounds()) {
            return;
        }

        final Minecraft gameClient = Minecraft.getInstance();
        final Camera camera = gameClient.gameRenderer.getMainCamera();

        // IslandCraft change (prompt 04): chance per tick from wind_gust_rate (gusts per second at full strength)
        final float chance = com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_GUST_RATE.get().floatValue() / 20f * Mth.lerp(strength, 0.2f, 1f);
        if (level.getRandom().nextFloat() >= chance) {
            return;
        }

        this.chooseSpotForWindSound(level, camera).ifPresent(
                soundPos -> {
                    BlockPos cameraPos = BlockPos.containing(camera.position());
                    boolean playAboveSound = soundPos.pos().getY() > cameraPos.getY() + 1
                            && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cameraPos).getY() > Mth.floor(cameraPos.getY());

                    // IslandCraft change (prompt 04): volume and pitch from the settings, strength and outdoor factor (original: 0.1/0.5 above, 0.5/1.0 otherwise)
                    float outdoor = com.paulorchard.islandcraft.weatherofarrakis.client.StormWindBed.outdoor();
                    float volume = com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_GUST_VOLUME.get().floatValue() * Mth.lerp(strength, 0.35f, 1f)
                            * Mth.lerp(outdoor, com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_INDOOR_VOLUME.get().floatValue(), 1f);
                    float pitch = com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_GUST_PITCH.get().floatValue() * (0.85f + 0.3f * level.getRandom().nextFloat());
                    if (playAboveSound) {
                        volume *= 0.2f;
                        pitch *= 0.55f;
                    }
                    gustsPlayed++;

                    level.playLocalSound(
                            soundPos.pos(),
                            soundPos.event(),
                            SoundSource.WEATHER,
                            volume, pitch,
                            false
                    );
                }
        );
    }

    private Optional<SoundPos> chooseSpotForWindSound(ClientLevel level, Camera camera) {
        BlockPos cameraPos = camera.blockPosition();

        RandomSource random = level.getRandom();

        int dx = random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        int dy = random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        int dz = random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        BlockPos blockPos = cameraPos.offset(dx, dy, dz);

        // IslandCraft change (prompt 04): wind_gust_sound overrides the biome sound
        String custom = com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.WIND_GUST_SOUND.get();
        net.minecraft.resources.Identifier customId = custom.isBlank() ? null : net.minecraft.resources.Identifier.tryParse(custom);
        if (customId != null) {
            return Optional.of(new SoundPos(SoundEvent.createVariableRangeEvent(customId), blockPos));
        }

        StormSounds sounds = level.environmentAttributes().getValue(ISEnvironmentAttributes.STORM_SOUNDS, cameraPos);

        return sounds.windSound().map(event -> new SoundPos(event.value(), blockPos));
    }

    private record SoundPos(SoundEvent event, BlockPos pos) {
    }
}