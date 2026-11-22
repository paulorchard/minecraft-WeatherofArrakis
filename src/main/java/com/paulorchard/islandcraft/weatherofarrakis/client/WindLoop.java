package com.paulorchard.islandcraft.weatherofarrakis.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Looping wind bed. Vanilla sound by reference (nether-wastes style air loop); volume follows the weather look. */
public final class WindLoop extends AbstractTickableSoundInstance {
    private float target;

    public WindLoop() {
        super(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value(), SoundSource.AMBIENT, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.pitch = 0.8f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    public void setTarget(float t) {
        this.target = t;
    }

    public float volumeNow() {
        return volume;
    }

    public void finish() {
        stop();
    }

    @Override
    public void tick() {
        volume += (target - volume) * 0.15f; // smooth, no clicks
    }
}
