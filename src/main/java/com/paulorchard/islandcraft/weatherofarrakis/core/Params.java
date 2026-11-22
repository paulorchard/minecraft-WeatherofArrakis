package com.paulorchard.islandcraft.weatherofarrakis.core;

/** All timing settings the state machine needs, as plain numbers (no game types, unit-testable). */
public record Params(
        boolean sandstormEnabled, int sandGapMinTicks, int sandGapMaxTicks,
        int sandDurationMinTicks, int sandDurationMaxTicks, int sandRampTicks,
        boolean coriolisEnabled, double gapDaysMin, double gapDaysMax,
        int approachTicks, int stormTicksMin, int stormTicksMax, int clearingTicks,
        int[] warningSeconds) {

    public static final long DAY_TICKS = 24000L;

    public long gapTicksMax() {
        return (long) (gapDaysMax * DAY_TICKS);
    }
}
