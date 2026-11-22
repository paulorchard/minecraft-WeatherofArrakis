package com.paulorchard.islandcraft.weatherofarrakis.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LevelWeatherTest {
    private static Params params() {
        return new Params(true, 1200, 2400, 1200, 2400, 100, true, 3.0, 6.0, 300 * 20, 40 * 20, 120 * 20, 30 * 20, new int[]{300, 240, 180, 120, 60, 30});
    }

    @Test
    void fullCycleSchedulesNextStorm() {
        Params p = params();
        LevelWeather w = new LevelWeather();
        Random r = new Random(1);
        List<WeatherEvent> ev = new ArrayList<>();
        w.startApproach(200, 100, false, ev);
        int ticks = 0;
        boolean sawStorm = false, sawClearing = false;
        while (w.phase != Phase.CALM || ticks == 0) {
            w.tick(p, 0, r);
            ticks++;
            sawStorm |= w.phase == Phase.STORM;
            sawClearing |= w.phase == Phase.CLEARING;
            assertTrue(ticks < 5000);
        }
        assertTrue(sawStorm && sawClearing);
        assertTrue(w.scheduledAt >= 3 * Params.DAY_TICKS && w.scheduledAt <= 6 * Params.DAY_TICKS);
    }

    @Test
    void passedScheduleStartsApproachAtOnce() {
        LevelWeather w = new LevelWeather();
        w.scheduledAt = 100;
        List<WeatherEvent> ev = w.tick(params(), 200, new Random(2));
        assertEquals(Phase.APPROACH, w.phase);
        assertTrue(ev.stream().anyMatch(e -> e instanceof WeatherEvent.PhaseChanged));
    }

    @Test
    void clockSetBackReschedules() {
        LevelWeather w = new LevelWeather();
        w.scheduledAt = 100 * Params.DAY_TICKS;
        w.tick(params(), 0, new Random(3));
        assertTrue(w.scheduledAt <= 6 * Params.DAY_TICKS);
    }

    @Test
    void warningsFireOnceAtThresholds() {
        Params p = params();
        LevelWeather w = new LevelWeather();
        List<WeatherEvent> all = new ArrayList<>();
        w.startApproach(300 * 20, 100, false, all);
        List<Integer> warned = new ArrayList<>();
        for (int i = 0; i < 300 * 20 + 5 && w.phase == Phase.APPROACH; i++)
            for (WeatherEvent e : w.tick(p, 0, new Random(4))) if (e instanceof WeatherEvent.Warning x) warned.add(x.secondsLeft());
        assertEquals(List.of(300, 240, 180, 120, 60, 30), warned);
    }

    @Test
    void stagesFollowTheFiveMinuteSchedule() {
        LevelWeather w = new LevelWeather();
        w.startApproach(300 * 20, 100, false, new ArrayList<>());
        assertEquals(1, w.approachStage());
        w.elapsedTicks = 61 * 20;
        assertEquals(2, w.approachStage());
        w.elapsedTicks = 271 * 20;
        assertEquals(6, w.approachStage());
    }

    @Test
    void sandstormCancelledByCoriolisAndNotStartedDuringIt() {
        Params p = params();
        LevelWeather w = new LevelWeather();
        List<WeatherEvent> ev = new ArrayList<>();
        w.startSandstorm(2000, ev);
        w.startApproach(100, 100, false, ev);
        assertFalse(w.sandActive);
        w.sandNextTicks = 1;
        for (int i = 0; i < 50; i++) w.tick(p, 0, new Random(5));
        assertFalse(w.sandActive);
    }

    @Test
    void majorPhaseNotResumed() {
        LevelWeather w = new LevelWeather();
        w.phase = Phase.MAJOR_WAIT;
        assertTrue(w.sanitizeAfterLoad());
        assertEquals(Phase.CALM, w.phase);
    }
}
