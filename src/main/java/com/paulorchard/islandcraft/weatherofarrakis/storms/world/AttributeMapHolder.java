// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.world;

import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import org.jspecify.annotations.NonNull;

public interface AttributeMapHolder {
    @NonNull
    default Biome2WeatherAttributeMapView islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap() {
        return Biome2WeatherAttributeMapView.EMPTY;
    }

    default void islandcraft_weatherofarrakis$rebindAttributeMap(final AdvancedSettings data) {
        // no-op by default
    }
}
