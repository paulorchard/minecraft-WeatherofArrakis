package com.paulorchard.islandcraft.weatherofarrakis.client;

import java.util.ArrayList;
import java.util.List;

/** Active camera shakes (no client-only classes). Each shake decays quadratically over its length. */
public final class ShakeClient {
    private record Shake(float strength, long startMs, long durMs) {}

    private static final List<Shake> ACTIVE = new ArrayList<>();

    private ShakeClient() {}

    public static synchronized void add(float strength, float seconds) {
        ACTIVE.add(new Shake(strength, net.minecraft.util.Util.getMillis(), (long) (seconds * 1000)));
    }

    public static synchronized void clear() {
        ACTIVE.clear();
    }

    /** Sum of the current strengths (0 when nothing is shaking). */
    public static synchronized float amplitude() {
        long now = net.minecraft.util.Util.getMillis();
        float sum = 0;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Shake s = ACTIVE.get(i);
            float t = (now - s.startMs) / (float) Math.max(1, s.durMs);
            if (t >= 1f) ACTIVE.remove(i);
            else sum += s.strength * (1 - t) * (1 - t);
        }
        return Math.min(sum, 1.5f);
    }
}
