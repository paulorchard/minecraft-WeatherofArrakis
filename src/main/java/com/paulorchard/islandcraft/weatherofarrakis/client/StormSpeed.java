package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/** Prompt 04 (our code): the dust speed factor and the measurement of real grain speed (blocks per second) for the once-a-second log. */
public final class StormSpeed {
    private static final int[] SAMPLE_AGES = {1, 20, 40};
    private static final double[] sum = new double[3];
    private static final int[] n = new int[3];
    private static int nearTicks, stuckEarly, born;
    private static double maxStep;
    public static String lastReport = "";

    private StormSpeed() {}

    /** Multiplier on the original velocity (-1 block per tick): setting x thunder lerp x rain strength. */
    public static float factor(ClientLevel level) {
        float rain = StormInput.rain(level, 1f);
        float thunder = StormInput.thunder(level, 1f);
        float strength = Mth.lerp(Mth.clamp((rain - 0.2f) / 0.8f, 0f, 1f), 0.4f, 1f);
        float thunderFactor = Mth.lerp(thunder, 1f, StormsClientConfig.PARTICLE_SPEED_THUNDER.get().floatValue());
        float coriolis = Mth.lerp(com.paulorchard.islandcraft.weatherofarrakis.StormOverride.extra, 1f, StormsClientConfig.CORIOLIS_STORM_SPEED.get().floatValue());
        return StormsClientConfig.PARTICLE_SPEED.get().floatValue() * thunderFactor * strength * coriolis;
    }

    /** Spawn-rate factor: 0.3 at the weakest strength, 1 at full (so a full sandstorm is unchanged), times the Coriolis density factor in the storm. */
    public static float density(ClientLevel level) {
        float rain = StormInput.rain(level, 1f);
        float strength = Mth.lerp(Mth.clamp((rain - 0.2f) / 0.8f, 0f, 1f), 0.3f, 1f);
        return strength * Mth.lerp(com.paulorchard.islandcraft.weatherofarrakis.StormOverride.extra, 1f, StormsClientConfig.CORIOLIS_STORM_DENSITY.get().floatValue());
    }

    /** Called from DustGrainParticle.tick after the move. dx, dz are the blocks moved this tick. */
    public static void sample(int age, double dx, double dz, double x, double y, double z) {
        double step = Math.sqrt(dx * dx + dz * dz);
        if (step > maxStep) maxStep = step;
        for (int i = 0; i < SAMPLE_AGES.length; i++) {
            if (age == SAMPLE_AGES[i]) {
                sum[i] += step * 20.0;
                n[i]++;
            }
        }
        if (age == 1) born++;
        if (age == 3 && Math.abs(dx) < 0.02) stuckEarly++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera().position().distanceToSqr(x, y, z) < 20 * 20) nearTicks++;
    }

    /** Once a second: formats and resets the sums. */
    public static String report() {
        StringBuilder sb = new StringBuilder();
        String[] names = {"t=0.05s", "t=1s", "t=2s"};
        for (int i = 0; i < 3; i++) {
            sb.append(names[i]).append('=').append(n[i] == 0 ? "-" : String.format(java.util.Locale.ROOT, "%.1f", sum[i] / n[i])).append(i < 2 ? " " : "");
            sum[i] = 0;
            n[i] = 0;
        }
        lastReport = String.format(java.util.Locale.ROOT, "grainSpeed(b/s) %s | born/s=%d stuckAfter3ticks=%d nearGrains(avg, <20 blocks)=%d maxStep(blocks/tick)=%.1f",
                sb, born, stuckEarly, nearTicks / 20, maxStep);
        born = stuckEarly = nearTicks = 0;
        maxStep = 0;
        return lastReport;
    }
}
