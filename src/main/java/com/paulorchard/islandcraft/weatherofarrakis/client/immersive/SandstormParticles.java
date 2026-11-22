/*
 * Adapted from Immersive Storms by TheDeathlyCow (https://github.com/TheDeathlyCow/immersive-storms, branch 26.1.x, commit 66c883b).
 * Copyright (C) TheDeathlyCow and contributors. Licensed under the GNU Lesser General Public License v3.0 or later (see LICENSE-LGPL-3.0.txt and NOTICE.md).
 * Adapted for IslandCraft - Weather of Arrakis (Paul Orchard): driven by the server's sandstorm intensity instead of vanilla rain; vanilla
 * DustParticleOptions instead of the custom dust_grain particle; wind direction, shader boost and config added.
 * This file is free software: you can redistribute it and/or modify it under the terms of the LGPL as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version. It is distributed WITHOUT ANY WARRANTY.
 */
package com.paulorchard.islandcraft.weatherofarrakis.client.immersive;

import com.paulorchard.islandcraft.weatherofarrakis.ClientConfig;
import com.paulorchard.islandcraft.weatherofarrakis.client.ClientDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Blowing sand: the original's grid scan (every column around the camera gets a small chance per tick, shifted upwind so the area looks full). */
public final class SandstormParticles {
    private static final float SCALE = 4f; // vanilla dust maximum (the original's custom particle uses 10)
    private static final float BASE_CHANCE = 1f / 60f;

    private SandstormParticles() {}

    /** Shader packs light the particles themselves, which turns them near-black; brighten the sand colour for them. */
    private static int lighten(int rgb) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * 1.9)), g = Math.min(255, (int) ((rgb >> 8 & 255) * 1.9)), b = Math.min(255, (int) ((rgb & 255) * 1.9));
        return r << 16 | g << 8 | b;
    }

    public static void tick(ClientLevel level, float intensity, float outdoor) {
        if (level.tickRateManager().isFrozen() || Minecraft.getInstance().isPaused()) return;
        float strength = intensity * outdoor;
        int radius = ClientConfig.SANDSTORM_PARTICLE_RADIUS.get();
        float density = ClientConfig.SANDSTORM_PARTICLE_SCALE.get().floatValue();
        boolean shaders = ShaderDetect.active();
        if (shaders && ClientConfig.SANDSTORM_BOOST_WITH_SHADERS.get()) density *= 2f;
        if (strength < 0.02f || radius <= 0 || density <= 0f) return;

        DustParticleOptions particle = new DustParticleOptions(shaders ? lighten(ClientConfig.sandColour()) : ClientConfig.sandColour(), SCALE);
        float rarity = BASE_CHANCE * density * strength;
        RandomSource random = level.getRandom();
        BlockPos cam = Minecraft.getInstance().gameRenderer.getMainCamera().blockPosition();
        double wx = StormLook.windX(), wz = StormLook.windZ();
        // the original shifts the scan area against the particle travel direction (it flies along +x); here against our wind vector
        int shiftX = (int) Math.round(-wx * radius / 2.0), shiftZ = (int) Math.round(-wz * radius / 2.0);
        double speed = 2.0 + 2.0 * intensity; // vanilla dust multiplies this by 0.1
        for (int x = cam.getX() - radius; x < cam.getX() + radius; x++) {
            for (int z = cam.getZ() - radius; z < cam.getZ() + radius; z++) {
                if (random.nextFloat() >= rarity) continue;
                int px = x + shiftX, pz = z + shiftZ;
                int y = cam.getY() + random.nextIntBetweenInclusive(-radius / 2, (radius + 1) / 2);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz));
                ClientDebug.particles++;
                level.addParticle(particle, px + random.nextDouble(), y + random.nextDouble(), pz + random.nextDouble(), wx * speed, 0, wz * speed);
            }
        }
    }
}
