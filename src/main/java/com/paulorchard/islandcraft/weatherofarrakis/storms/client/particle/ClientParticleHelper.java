// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle;

import com.paulorchard.islandcraft.weatherofarrakis.storms.client.util.ClientServicesHelper;
import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public interface ClientParticleHelper {
    <T extends ParticleOptions> void registerProvider(ParticleType<T> type, ParticleHelper.ProviderFactory<T> provider);

    static ClientParticleHelper getInstance() {
        return ClientServicesHelper.CLIENT_PARTICLE_HELPER;
    }
}