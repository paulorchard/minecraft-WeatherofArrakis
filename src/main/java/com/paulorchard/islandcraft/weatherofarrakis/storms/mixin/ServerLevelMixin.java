// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.mixin;

import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.AttributeMapHolder;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.Biome2WeatherAttributeMapView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements AttributeMapHolder {
    @Shadow
    public abstract MinecraftServer getServer();

    @Override
    public void islandcraft_weatherofarrakis$rebindAttributeMap(AdvancedSettings data) {
        this.getServer().islandcraft_weatherofarrakis$rebindAttributeMap(data);
    }

    @Override
    public @NonNull Biome2WeatherAttributeMapView islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap() {
        return this.getServer().islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap();
    }
}