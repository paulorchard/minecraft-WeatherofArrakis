// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.StormFogModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @Shadow
    private float rainFogMultiplier;

    @WrapMethod(method = "setupFog")
    private void setStormFogDistanceOverride(
            FogData fog,
            Camera camera,
            ClientLevel level,
            float renderDistance,
            DeltaTracker deltaTracker,
            Operation<Void> original
    ) {
        original.call(fog, camera, level, renderDistance, deltaTracker);

        // IslandCraft change (prompt 04): the Coriolis override also opens this gate (original: if (this.rainFogMultiplier > 0f))
        if (this.rainFogMultiplier > 0f || com.paulorchard.islandcraft.weatherofarrakis.StormOverride.rain > 0f) {
            StormFogModifier.applyStormFogDistance(fog, camera, level, deltaTracker);
        }
    }
}