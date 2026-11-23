package com.paulorchard.islandcraft.weatherofarrakis.storms.mixin;

import com.paulorchard.islandcraft.weatherofarrakis.StormOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prompt 04 (our code, not upstream): WeatherAttributes.WeatherAccess.from(level) is what vanilla's built-in weather layers (sky and fog colour, sky light) and the
 * copied biome weather layers read the rain and thunder gradients from. Returning the larger of the vanilla gradient and the Coriolis override makes all of them
 * follow the Coriolis storm.
 */
@Mixin(targets = "net.minecraft.world.attribute.WeatherAttributes$WeatherAccess$1")
public class StormWeatherAccessMixin {
    @Inject(method = "rainLevel", at = @At("RETURN"), cancellable = true)
    private void islandcraft_weatherofarrakis$rain(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(StormOverride.rain(cir.getReturnValueF()));
    }

    @Inject(method = "thunderLevel", at = @At("RETURN"), cancellable = true)
    private void islandcraft_weatherofarrakis$thunder(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(StormOverride.thunder(cir.getReturnValueF()));
    }
}
