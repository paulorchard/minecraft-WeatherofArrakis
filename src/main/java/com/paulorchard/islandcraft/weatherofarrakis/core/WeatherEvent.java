package com.paulorchard.islandcraft.weatherofarrakis.core;

/** Things that happened during one tick of a {@link LevelWeather}; the server layer turns them into chat, sync and listeners. */
public sealed interface WeatherEvent {
    record PhaseChanged(Phase from, Phase to) implements WeatherEvent {}

    /** A chat warning. {@code secondsLeft} is the real time left of the approach, {@code first} marks the opening message. */
    record Warning(int secondsLeft, boolean first) implements WeatherEvent {}

    record SandstormStarted(int durationTicks) implements WeatherEvent {}

    record SandstormEnded(boolean cancelledByCoriolis) implements WeatherEvent {}
}
