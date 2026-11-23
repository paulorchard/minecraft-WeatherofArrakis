// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config.section;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.Translate;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.ConfigClassHandler;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.SerialEntry;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.AutoGen;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.FloatSlider;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.IntSlider;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.TickBox;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.GsonConfigSerializerBuilder;

import java.nio.file.Path;

public class SandstormSettings {
    public static final Path PATH = ImmersiveStorms.getConfigDir().resolve("sandstorm.json5");

    public static final ConfigClassHandler<SandstormSettings> HANDLER = ConfigClassHandler.createBuilder(SandstormSettings.class)
            .id(ImmersiveStorms.id("sandstorm"))
            .serializer(
                    config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(PATH)
                            .setJson5(true)
                            .build()
            )
            .build();

    private static final String CATEGORY = "sandstorms";

    @AutoGen(category = CATEGORY)
    @Translate.Name("Enable sandstorm particles")
    @Translate.NoComment
    @TickBox
    @SerialEntry
    boolean enableSandstormParticles = true;

    @AutoGen(category = CATEGORY)
    @Translate.Name("Detect Particle Rain")
    @TickBox
    @SerialEntry(comment = "Will disable sandstorm particles and sounds automatically if the mod Particle Rain is detected")
    boolean detectParticleRain = true;

    @AutoGen(category = CATEGORY)
    @Translate.Name("Sandstorm particle render distance")
    @IntSlider(min = 1, max = 100, step = 1)
    @SerialEntry(comment = "How many blocks away to render sandstorm particles, must be positive")
    int sandstormParticleRenderDistance = 20;

    @AutoGen(category = CATEGORY)
    @Translate.Name("Sandstorm particle density")
    @FloatSlider(min = 0.1f, max = 10.0f, step = 0.1f, format = "%.2f")
    @SerialEntry(comment = "Multiplier for how frequently sandstorm particles should appear, must be positive. Bigger values = more common.")
    float sandstormParticleDensityMultiplier = 1.0f;

    @AutoGen(category = CATEGORY)
    @Translate.Name("Dust particle size")
    @FloatSlider(min = 0.01f, max = 2.0f, step = 0.05f, format = "%.2f")
    @SerialEntry(comment = "Multiplies the size of the dust particles. Does not affect the ambient wind particles.")
    float dustParticleSize = 1.0f;

    public boolean isEnableSandstormParticles() {
        return enableSandstormParticles;
    }

    public boolean isDetectParticleRain() {
        return detectParticleRain;
    }

    // IslandCraft change (prompt 04): density and size read the live client config instead of the in-memory field.
    public int getSandstormParticleRenderDistance() {
        return sandstormParticleRenderDistance;
    }

    public float getSandstormParticleDensityMultiplier() {
        return (float) (double) com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_DENSITY.get();
    }

    public float dustParticleSize() {
        return (float) (double) com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig.PARTICLE_SIZE.get();
    }
}