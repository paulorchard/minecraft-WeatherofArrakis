package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.core.LevelWeather;
import com.paulorchard.islandcraft.weatherofarrakis.core.Phase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;

/** Read-only view of the weather for other mods (Worms, Water, Vehicles). All methods are for the server thread. */
public final class StormQuery {
    /** Called on the server thread whenever a level's Coriolis phase changes. */
    @FunctionalInterface
    public interface PhaseListener {
        void onPhaseChange(ServerLevel level, Phase from, Phase to);
    }

    private static final List<PhaseListener> LISTENERS = new CopyOnWriteArrayList<>();

    private StormQuery() {}

    public static void addPhaseListener(PhaseListener l) {
        LISTENERS.add(l);
    }

    public static void removePhaseListener(PhaseListener l) {
        LISTENERS.remove(l);
    }

    static void firePhaseChange(ServerLevel level, Phase from, Phase to) {
        for (PhaseListener l : LISTENERS) l.onPhaseChange(level, from, to);
    }

    /** Coriolis phase of the level; CALM for a level outside the configured dimensions. */
    public static Phase coriolisPhase(ServerLevel level) {
        return WeatherConfig.isQualifying(level) ? WeatherManager.state(level).phase : Phase.CALM;
    }

    /** Seconds left in the current Coriolis phase (0 in CALM). */
    public static int secondsRemaining(ServerLevel level) {
        if (!WeatherConfig.isQualifying(level)) return 0;
        return (WeatherManager.state(level).phaseTicksLeft() + 19) / 20;
    }

    /** Sandstorm intensity 0..1 now (fades in and out). */
    public static double sandstormIntensity(ServerLevel level) {
        if (!WeatherConfig.isQualifying(level)) return 0;
        LevelWeather w = WeatherManager.state(level);
        return w.sandstormIntensity(WeatherConfig.params().sandRampTicks());
    }
}
