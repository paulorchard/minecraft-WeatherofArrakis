// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.neoforge.client;

import com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.ClientParticleHelper;
import com.paulorchard.islandcraft.weatherofarrakis.storms.particle.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.ArrayList;
import java.util.List;

public class ClientParticleHelperImpl implements ClientParticleHelper {
    public static final List<ProviderPair<?>> REGISTRY = new ArrayList<>();

    @Override
    public <T extends ParticleOptions> void registerProvider(ParticleType<T> type, ParticleHelper.ProviderFactory<T> provider) {
        REGISTRY.add(new ProviderPair<>(type, provider));
    }

    public record ProviderPair<T extends ParticleOptions>(ParticleType<T> type, ParticleHelper.ProviderFactory<T> provider) {
        public void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(type, provider::create);
        }
    }
}