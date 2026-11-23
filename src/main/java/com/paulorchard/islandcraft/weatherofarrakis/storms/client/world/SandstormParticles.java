// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.world;

import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.SandstormSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.util.ISClientTickEvents;
import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.DustGrainParticleEffect;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISEnvironmentAttributes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.util.ISMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

public final class SandstormParticles implements ISClientTickEvents.EndLevel {
    public static final Vector3f COLOR = ISMath.unpackRgb(BiomeWindEffects.ParticleColor.SANDY.color());

    private static final float PARTICLE_SCALE = 10f;
    private static final float PARTICLE_VELOCITY = -1f;
    private static final float BASE_PARTICLE_CHANCE = 1f / 60f;

    @Override
    public void onEndTick(ClientLevel level) {
        if (!level.isRaining() || level.tickRateManager().isFrozen()) {
            return;
        }

        SandstormSettings settings = SandstormSettings.HANDLER.instance();
        final int renderDistance = settings.getSandstormParticleRenderDistance();
        boolean enabled = settings.isEnableSandstormParticles()
                && renderDistance > 0;

        if (!enabled) {
            return;
        }

        final Minecraft gameClient = Minecraft.getInstance();
        final Camera camera = gameClient.gameRenderer.getMainCamera();

        // main particle loop
        final BlockPos cameraPos = camera.blockPosition();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final float particleSize = PARTICLE_SCALE * settings.dustParticleSize();
        final ParticleOptions particle = new DustGrainParticleEffect(COLOR, particleSize);
        final float rarity = BASE_PARTICLE_CHANCE * settings.getSandstormParticleDensityMultiplier();
        final int cameraY = cameraPos.getY();
        final int xOffset = renderDistance / 2;

        // IslandCraft change (prompt 04): the velocity was the constant PARTICLE_VELOCITY; it is now a setting scaled by storm strength.
        final float velocity = PARTICLE_VELOCITY * com.paulorchard.islandcraft.weatherofarrakis.client.StormSpeed.factor(level);
        for (int x = cameraPos.getX() - renderDistance; x < cameraPos.getX() + renderDistance; x++) {
            for (int z = cameraPos.getZ() - renderDistance; z < cameraPos.getZ() + renderDistance; z++) {
                // adjust to account for the fact that particles travel along the x-axis
                // makes the area the particles come from look less empty
                int adjustedX = x + xOffset;

                int y = cameraY + level.getRandom().nextIntBetweenInclusive(-renderDistance / 2, (renderDistance + 1) / 2);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING, adjustedX, z));

                pos.set(adjustedX, y, z);
                addParticle(level, particle, pos, rarity, velocity);
            }
        }
    }

    private static void addParticle(ClientLevel level, ParticleOptions particle, BlockPos pos, float rarity, float velocity) {
        Holder<Biome> biome = level.getBiomeManager().getNoiseBiomeAtPosition(pos);

        RandomSource random = level.getRandom();

        boolean addParticle = random.nextFloat() < rarity
                && biome.value().getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.NONE
                && level.environmentAttributes().getValue(ISEnvironmentAttributes.HAS_SANDSTORM_DUST, pos);

        if (addParticle) {
            level.addParticle(
                    particle,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    velocity, 0, 0
            );
        }
    }
}