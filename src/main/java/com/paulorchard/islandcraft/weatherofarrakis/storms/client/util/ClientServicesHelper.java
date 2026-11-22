// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.util;

import com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.ClientParticleHelper;
import com.paulorchard.islandcraft.weatherofarrakis.storms.util.ServicesHelper;

public final class ClientServicesHelper {
    public static final ClientParticleHelper CLIENT_PARTICLE_HELPER = ServicesHelper.load(ClientParticleHelper.class);

    private ClientServicesHelper() {

    }
}