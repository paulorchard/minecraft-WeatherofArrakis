package com.paulorchard.islandcraft.weatherofarrakis.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Prompt 04 (our code): a non-positional (head-relative) sound in the WEATHER category whose volume and pitch the owner of the instance sets every tick. */
public class StormSoundInstance extends AbstractTickableSoundInstance {
    private boolean halted;
    /** Volume and pitch are written by whoever owns the instance (the bed, /storm_sound). */
    public float targetVolume, targetPitch;

    public StormSoundInstance(SoundEvent event, float volume, float pitch, boolean loop) {
        super(event, SoundSource.WEATHER, RandomSource.create());
        this.looping = loop;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.targetVolume = this.volume = volume;
        this.targetPitch = this.pitch = pitch;
    }

    @Override
    public void tick() {
        this.volume = targetVolume;
        this.pitch = targetPitch;
    }

    /** Stops this instance now. */
    public void halt() {
        halted = true;
        stop();
    }

    public boolean isHalted() {
        return halted;
    }

    public float currentVolume() {
        return volume;
    }
}
