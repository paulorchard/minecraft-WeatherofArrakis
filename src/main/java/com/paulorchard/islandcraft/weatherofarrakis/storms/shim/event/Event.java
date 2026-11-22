package com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Stand-in for dev.yumi.commons.event.Event: register listeners, call invoker(). Our own code, not adapted from upstream. */
public final class Event<I, T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<List<T>, T> factory;
    private T invoker;

    Event(Function<List<T>, T> factory) {
        this.factory = factory;
        this.invoker = factory.apply(listeners);
    }

    public void register(T listener) {
        listeners.add(listener);
    }

    public T invoker() {
        return invoker;
    }
}
