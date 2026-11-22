// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.world;

import net.minecraft.client.renderer.WeatherEffectRenderer;

public interface WeatherRenderStateExtension {
    void immersiveStorms$addBlackRainInstance(WeatherEffectRenderer.ColumnInstance instance);

    boolean immersiveStorms$isBlackRainInstance(WeatherEffectRenderer.ColumnInstance instance);
}
