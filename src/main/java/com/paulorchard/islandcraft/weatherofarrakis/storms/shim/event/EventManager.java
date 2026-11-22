package com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event;

import java.util.List;
import java.util.function.Function;

/** Stand-in for dev.yumi.commons.event.EventManager. Our own code, not adapted from upstream. */
public final class EventManager<I> {
    public EventManager(I defaultPhase, Function<String, I> parser) {}

    public <T> Event<I, T> create(Class<T> type, Function<List<T>, T> factory) {
        return new Event<>(factory);
    }
}
