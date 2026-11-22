// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.mixin;

import com.paulorchard.islandcraft.weatherofarrakis.storms.world.Biome2WeatherAttributeMapView;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.WeatherAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public class EnvironmentAttributeSystemMixin {
    @Inject(
            method = "addDefaultLayers",
            at = @At("TAIL")
    )
    private static void injectWeatherLayers(EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        if (level.canHaveWeather()) {
            Biome2WeatherAttributeMapView.addDefaultLayers(
                    builder,
                    level.registryAccess().lookupOrThrow(Registries.ENVIRONMENT_ATTRIBUTE),
                    WeatherAttributes.WeatherAccess.from(level),
                    Biome2WeatherAttributeMapView.AttributeAccess.from(level),
                    level.getBiomeManager()
            );
        }
    }
}