// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.client.mixin.client;

import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.Biome2WeatherAttributeMapView;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.AttributeMapHolder;
import com.paulorchard.islandcraft.weatherofarrakis.storms.world.Biome2WeatherAttributeMapImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements AttributeMapHolder {
    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();

    @Unique
    private final Biome2WeatherAttributeMapView immersiveStorms_attributeMap = new Biome2WeatherAttributeMapImpl();

    @Inject(
            method = "<init>", at = @At("TAIL")
    )
    private void onInit(CallbackInfo ci) {
        AdvancedSettings.HANDLER.load();
        this.islandcraft_weatherofarrakis$rebindAttributeMap(AdvancedSettings.HANDLER.instance());
    }

    @Override
    @Unique
    @NonNull
    public Biome2WeatherAttributeMapView islandcraft_weatherofarrakis$getBiomeToWeatherAttributeMap() {
        return this.immersiveStorms_attributeMap;
    }

    @Override
    @Unique
    public void islandcraft_weatherofarrakis$rebindAttributeMap(AdvancedSettings data) {
        immersiveStorms_attributeMap.clear();
        this.immersiveStorms_attributeMap.rebind(this.registryAccess().lookupOrThrow(Registries.BIOME), data);
    }
}