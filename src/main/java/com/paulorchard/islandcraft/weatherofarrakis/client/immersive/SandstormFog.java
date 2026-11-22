/*
 * Adapted from Immersive Storms by TheDeathlyCow (https://github.com/TheDeathlyCow/immersive-storms, branch 26.1.x, commit 66c883b).
 * Copyright (C) TheDeathlyCow and contributors. Licensed under the GNU Lesser General Public License v3.0 or later (see LICENSE-LGPL-3.0.txt and NOTICE.md).
 * Adapted for IslandCraft - Weather of Arrakis (Paul Orchard): the original's StormFogModifier (fog end, sky end and cloud end all pulled in together)
 * driven by our intensity, with configured start/end/colour, applied through ViewportEvent.RenderFog instead of an AtmosphericFogEnvironment mixin.
 * This file is free software: you can redistribute it and/or modify it under the terms of the LGPL as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version. It is distributed WITHOUT ANY WARRANTY.
 */
package com.paulorchard.islandcraft.weatherofarrakis.client.immersive;

import com.paulorchard.islandcraft.weatherofarrakis.ClientConfig;
import com.paulorchard.islandcraft.weatherofarrakis.client.ClientDebug;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Fog for the sandstorm: distance and colour pulled towards the configured values by intensity x outdoor. Most shader packs ignore it. */
public final class SandstormFog {
    private SandstormFog() {}

    public static void apply(ViewportEvent.RenderFog e, float k) {
        if (k <= 0.001f || !ClientConfig.SANDSTORM_FOG.get()) return;
        var d = e.getFogData();
        float endCfg = ClientConfig.SANDSTORM_FOG_END.get().floatValue();
        float startCfg = Math.min(ClientConfig.SANDSTORM_FOG_START.get().floatValue(), endCfg * 0.9f);
        float end = Mth.lerp(k, d.environmentalEnd, Math.min(d.environmentalEnd, endCfg));
        float start = Mth.lerp(k, d.environmentalStart, Math.min(d.environmentalStart, startCfg));
        ClientDebug.fogApplied++;
        ClientDebug.fogEnd = end;
        e.setFarPlaneDistance(end);
        e.setNearPlaneDistance(start);
        d.renderDistanceEnd = Math.min(d.renderDistanceEnd, Mth.lerp(k, d.renderDistanceEnd, end));
        d.renderDistanceStart = Math.min(d.renderDistanceStart, Mth.lerp(k, d.renderDistanceStart, start));
        d.skyEnd = Math.min(d.skyEnd, Mth.lerp(k, d.skyEnd, end));
        d.cloudEnd = Math.min(d.cloudEnd, Mth.lerp(k, d.cloudEnd, end));
    }

    public static void colour(ViewportEvent.ComputeFogColor e, float k) {
        if (k <= 0.001f || !ClientConfig.SANDSTORM_FOG.get()) return;
        int rgb = ClientConfig.sandColour();
        float luma = Mth.clamp(0.2126f * e.getRed() + 0.7152f * e.getGreen() + 0.0722f * e.getBlue(), 0f, 1f);
        float bright = Mth.clamp(0.15f + luma * 1.1f, 0f, 1f); // vanilla daylight keeps nights dark
        e.setRed(Mth.lerp(k, e.getRed(), ((rgb >> 16 & 255) / 255f) * bright));
        e.setGreen(Mth.lerp(k, e.getGreen(), ((rgb >> 8 & 255) / 255f) * bright));
        e.setBlue(Mth.lerp(k, e.getBlue(), ((rgb & 255) / 255f) * bright));
    }
}
