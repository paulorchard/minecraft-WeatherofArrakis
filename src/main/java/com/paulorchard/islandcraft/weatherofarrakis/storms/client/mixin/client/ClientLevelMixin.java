// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.mixin.client;

import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.AttributeMapHolder;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.Biome2WeatherAttributeMapView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements AttributeMapHolder {
    @Shadow
    @Final
    private ClientPacketListener connection;

    @Override
    public void islandcraft_weatherofarrakis$rebindAttributeMap(AdvancedSettings data) {
        this.connection.islandcraft_weatherofarrakis$rebindAttributeMap(data);
    }

    @Override
    public @NonNull Biome2WeatherAttributeMapView islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap() {
        return this.connection.islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap();
    }
}