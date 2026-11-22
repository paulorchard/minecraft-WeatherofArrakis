// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.registry;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ISSoundEvents {
    public static final Holder<SoundEvent> WEATHER_STRONG_WIND = registerHolder("weather.strong_wind");
    public static final Holder<SoundEvent> WEATHER_SOFT_WIND = registerHolder("weather.soft_wind");

    public static void initialize() {
        ImmersiveStorms.LOGGER.debug("Initialized Immersive Storms sound events");
    }

    private static Holder<SoundEvent> registerHolder(String name) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(ImmersiveStorms.id(name));

        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, event.location(), event);
    }

    private ISSoundEvents() {

    }
}