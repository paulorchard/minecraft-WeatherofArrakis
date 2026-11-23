// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle;

import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.DustGrainParticleEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public class DustGrainParticle extends DustParticleBase<DustGrainParticleEffect> {
    public DustGrainParticle(
            ClientLevel world,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            DustGrainParticleEffect parameters,
            SpriteSet spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ, parameters, spriteProvider);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.gravity = 1e-1f;
        // IslandCraft change (prompt 04): friction and lifetime are settings; the measurement hooks are in StormSpeed.
        this.friction = (float) (double) com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_FRICTION.get();
        this.lifetime = Math.max(1, (int) (this.lifetime * com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_LIFETIME_SCALE.get()));

        float multiplier = this.random.nextFloat() * 0.4F + 0.6F;
        this.rCol = this.randomizeColor(parameters.getColor().x(), multiplier);
        this.gCol = this.randomizeColor(parameters.getColor().y(), multiplier);
        this.bCol = this.randomizeColor(parameters.getColor().z(), multiplier);
    }

    // IslandCraft change (prompt 04): measurement of real grain speed for the debug log.
    @Override
    public void tick() {
        double px = this.x, pz = this.z;
        super.tick();
        com.paulorchard.islandcraft.weatherofarrakis.client.StormSpeed.sample(this.age, this.x - px, this.z - pz, this.x, this.y, this.z);
    }

    public static class Provider implements ParticleProvider<DustGrainParticleEffect> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public @Nullable Particle createParticle(
                DustGrainParticleEffect parameters,
                ClientLevel world,
                double x, double y, double z,
                double velocityX, double velocityY, double velocityZ,
                RandomSource random
        ) {
            return new DustGrainParticle(
                    world,
                    x, y, z,
                    velocityX, velocityY, velocityZ,
                    parameters,
                    this.sprite
            );
        }
    }
}