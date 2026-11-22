package com.paulorchard.islandcraft.weatherofarrakis.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weather state of one level and its transitions. No game types: the server layer feeds it the clock and acts on the returned events.
 * Active phases count ticks (real seconds while the server runs); the next storm is a clock instant (sleeping brings it closer).
 */
public final class LevelWeather {
    public Phase phase = Phase.CALM;
    /** Overworld clock tick of the next Coriolis storm, or -1 when none is scheduled. */
    public long scheduledAt = -1;
    public int elapsedTicks;
    public int approachTicks;
    public int stormTicks;
    public int clearingTicks;
    public boolean major;

    public boolean sandActive;
    public int sandTicksLeft;
    public int sandTotalTicks;
    /** Countdown to the next sandstorm in ticks, -1 when not rolled. */
    public int sandNextTicks = -1;

    /** Not saved. MAX_VALUE = approach just started (opening message due); -1 = just loaded (do not repeat warnings). */
    private int lastRemainingSeconds = -1;

    public LevelWeather() {}

    public LevelWeather(Phase phase, long scheduledAt, int elapsedTicks, int approachTicks, int stormTicks, int clearingTicks, boolean major,
                        boolean sandActive, int sandTicksLeft, int sandTotalTicks, int sandNextTicks) {
        this.phase = phase;
        this.scheduledAt = scheduledAt;
        this.elapsedTicks = elapsedTicks;
        this.approachTicks = approachTicks;
        this.stormTicks = stormTicks;
        this.clearingTicks = clearingTicks;
        this.major = major;
        this.sandActive = sandActive;
        this.sandTicksLeft = sandTicksLeft;
        this.sandTotalTicks = sandTotalTicks;
        this.sandNextTicks = sandNextTicks;
    }

    public int phaseTotalTicks() {
        return switch (phase) {
            case APPROACH -> approachTicks;
            case STORM -> stormTicks;
            case CLEARING -> clearingTicks;
            default -> 0;
        };
    }

    public int phaseTicksLeft() {
        return Math.max(0, phaseTotalTicks() - elapsedTicks);
    }

    /** Sandstorm intensity 0..1 including the fade in and out. */
    public double sandstormIntensity(int rampTicks) {
        if (!sandActive || sandTotalTicks <= 0) return 0;
        int done = sandTotalTicks - sandTicksLeft;
        double in = rampTicks <= 0 ? 1 : Math.min(1.0, done / (double) rampTicks);
        double out = rampTicks <= 0 ? 1 : Math.min(1.0, sandTicksLeft / (double) rampTicks);
        return Math.max(0, Math.min(in, out));
    }

    /** Remaining fractions of a 300 s approach at which stages 1..6 begin: 5:00 4:00 3:00 2:00 1:00 0:30. */
    private static final double[] STAGE_START = {1.0, 0.8, 0.6, 0.4, 0.2, 0.1};

    /** Stage 1..6 of the approach, scaled so a shortened approach goes through the same six looks. 0 outside APPROACH. */
    public int approachStage() {
        if (phase != Phase.APPROACH || approachTicks <= 0) return 0;
        return stageForRemainingFraction(1.0 - elapsedTicks / (double) approachTicks);
    }

    public static int stageForRemainingFraction(double remaining) {
        int stage = 1;
        for (int i = 0; i < STAGE_START.length; i++) if (remaining <= STAGE_START[i] + 1e-9) stage = i + 1;
        return stage;
    }

    /** Length in ticks of the given stage (1..6) of this approach. */
    public int stageLengthTicks(int stage) {
        double end = stage >= 6 ? 0.0 : STAGE_START[stage];
        return (int) Math.round((STAGE_START[stage - 1] - end) * approachTicks);
    }

    public void startApproach(int approachTicks, int stormTicks, boolean major, List<WeatherEvent> out) {
        Phase from = phase;
        if (sandActive) {
            sandActive = false;
            sandTicksLeft = 0;
            out.add(new WeatherEvent.SandstormEnded(true));
        }
        this.approachTicks = Math.max(1, approachTicks);
        this.stormTicks = Math.max(1, stormTicks);
        this.major = major;
        this.elapsedTicks = 0;
        this.lastRemainingSeconds = Integer.MAX_VALUE;
        this.phase = Phase.APPROACH;
        out.add(new WeatherEvent.PhaseChanged(from, Phase.APPROACH));
    }

    /** Ends any active phase at once and returns to CALM (the ordinary schedule is kept). */
    public void stop(List<WeatherEvent> out) {
        if (phase == Phase.CALM) return;
        Phase from = phase;
        phase = Phase.CALM;
        elapsedTicks = 0;
        major = false;
        out.add(new WeatherEvent.PhaseChanged(from, Phase.CALM));
    }

    public void scheduleNext(Params p, long clockNow, Random rng) {
        double days = p.gapDaysMin() + rng.nextDouble() * Math.max(0, p.gapDaysMax() - p.gapDaysMin());
        scheduledAt = clockNow + (long) (days * Params.DAY_TICKS);
    }

    public void startSandstorm(int durationTicks, List<WeatherEvent> out) {
        sandActive = true;
        sandTotalTicks = Math.max(1, durationTicks);
        sandTicksLeft = sandTotalTicks;
        out.add(new WeatherEvent.SandstormStarted(sandTotalTicks));
    }

    public void stopSandstorm(List<WeatherEvent> out) {
        if (!sandActive) return;
        sandActive = false;
        sandTicksLeft = 0;
        sandNextTicks = -1;
        out.add(new WeatherEvent.SandstormEnded(false));
    }

    private static int between(Random rng, int lo, int hi) {
        return hi <= lo ? lo : lo + rng.nextInt(hi - lo + 1);
    }

    /** One server tick. */
    public List<WeatherEvent> tick(Params p, long clockNow, Random rng) {
        List<WeatherEvent> out = new ArrayList<>(2);
        switch (phase) {
            case CALM -> tickCalm(p, clockNow, rng, out);
            case APPROACH -> {
                elapsedTicks++;
                int remaining = (approachTicks - elapsedTicks + 19) / 20;
                warn(p, remaining, out);
                if (elapsedTicks >= approachTicks) {
                    elapsedTicks = 0;
                    phase = Phase.STORM;
                    out.add(new WeatherEvent.PhaseChanged(Phase.APPROACH, Phase.STORM));
                }
            }
            case STORM -> {
                elapsedTicks++;
                if (elapsedTicks >= stormTicks && major) {
                    elapsedTicks = 0;
                    phase = Phase.MAJOR_RESET;
                    out.add(new WeatherEvent.PhaseChanged(Phase.STORM, Phase.MAJOR_RESET));
                } else if (elapsedTicks >= stormTicks) {
                    elapsedTicks = 0;
                    clearingTicks = p.clearingTicks();
                    phase = Phase.CLEARING;
                    out.add(new WeatherEvent.PhaseChanged(Phase.STORM, Phase.CLEARING));
                }
            }
            case CLEARING -> {
                elapsedTicks++;
                if (elapsedTicks >= clearingTicks) {
                    elapsedTicks = 0;
                    phase = Phase.CALM;
                    major = false;
                    scheduleNext(p, clockNow, rng);
                    out.add(new WeatherEvent.PhaseChanged(Phase.CLEARING, Phase.CALM));
                }
            }
            default -> { /* MAJOR_RESET / MAJOR_WAIT are driven by the Major storm controller */ }
        }
        return out;
    }

    private void warn(Params p, int remaining, List<WeatherEvent> out) {
        if (lastRemainingSeconds == Integer.MAX_VALUE) { // approach just started: opening message with the real time left
            lastRemainingSeconds = remaining;
            out.add(new WeatherEvent.Warning(Math.max(1, (approachTicks + 19) / 20), true));
            return;
        }
        if (lastRemainingSeconds < 0) { // resumed after a restart: do not repeat
            lastRemainingSeconds = remaining;
            return;
        }
        for (int w : p.warningSeconds()) {
            if (w < approachTicks / 20.0 && lastRemainingSeconds > w && remaining <= w) out.add(new WeatherEvent.Warning(w, false));
        }
        lastRemainingSeconds = remaining;
    }

    private void tickCalm(Params p, long clockNow, Random rng, List<WeatherEvent> out) {
        if (p.coriolisEnabled()) {
            if (scheduledAt < 0 || scheduledAt - clockNow > p.gapTicksMax() + 1200) {
                scheduleNext(p, clockNow, rng);
            } else if (clockNow >= scheduledAt) {
                startApproach(p.approachTicks(), between(rng, p.stormTicksMin(), p.stormTicksMax()), false, out);
                return;
            }
        }
        if (!p.sandstormEnabled()) {
            stopSandstorm(out);
            return;
        }
        if (sandActive) {
            if (--sandTicksLeft <= 0) {
                sandActive = false;
                sandNextTicks = -1;
                out.add(new WeatherEvent.SandstormEnded(false));
            }
        } else if (sandNextTicks < 0) {
            sandNextTicks = between(rng, p.sandGapMinTicks(), p.sandGapMaxTicks());
        } else if (--sandNextTicks <= 0) {
            sandNextTicks = -1;
            startSandstorm(between(rng, p.sandDurationMinTicks(), p.sandDurationMaxTicks()), out);
        }
    }

    /** Major storm: the reset pass is over (or the window ran out); the level waits for logins to reopen. */
    public void toMajorWait(List<WeatherEvent> out) {
        if (phase != Phase.MAJOR_RESET) return;
        phase = Phase.MAJOR_WAIT;
        elapsedTicks = 0;
        out.add(new WeatherEvent.PhaseChanged(Phase.MAJOR_RESET, Phase.MAJOR_WAIT));
    }

    /** Major storm over: CALM. The ordinary storm date is kept if still in the future and re-rolled if it has gone by. */
    public void endMajor(Params p, long clockNow, Random rng, List<WeatherEvent> out) {
        Phase from = phase;
        phase = Phase.CALM;
        elapsedTicks = 0;
        major = false;
        if (scheduledAt < 0 || scheduledAt <= clockNow) scheduleNext(p, clockNow, rng);
        if (from != Phase.CALM) out.add(new WeatherEvent.PhaseChanged(from, Phase.CALM));
    }

    /** After load: Major phases cannot be resumed and become CALM. Returns true if that happened. */
    public boolean sanitizeAfterLoad() {
        lastRemainingSeconds = -1;
        if (phase == Phase.MAJOR_RESET || phase == Phase.MAJOR_WAIT) {
            phase = Phase.CALM;
            elapsedTicks = 0;
            major = false;
            return true;
        }
        return false;
    }
}
