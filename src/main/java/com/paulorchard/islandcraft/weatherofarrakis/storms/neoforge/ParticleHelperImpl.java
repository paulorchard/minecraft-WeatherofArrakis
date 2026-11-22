// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.neoforge;

import com.mojang.serialization.MapCodec;
import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ParticleHelperImpl implements ParticleHelper {
    @Override
    public <T extends ParticleOptions> ParticleType<T> createComplex(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> networkCodec) {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return networkCodec;
            }
        };
    }

    @Override
    public SimpleParticleType createSimple(boolean alwaysSpawn) {
        return new SimpleParticleType(alwaysSpawn) { };
    }
}