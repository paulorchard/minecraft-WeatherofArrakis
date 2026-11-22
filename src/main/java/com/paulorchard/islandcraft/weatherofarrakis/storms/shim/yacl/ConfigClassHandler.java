package com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl;

import java.util.function.Function;
import net.minecraft.resources.Identifier;

/**
 * Stand-in for YACL's ConfigClassHandler (YACL is not a dependency). Backed by a plain in-memory instance holding the upstream defaults:
 * load() and save() do not touch any file. Our own code, not adapted from upstream.
 */
public final class ConfigClassHandler<T> {
    private final Class<T> type;
    private T instance;
    private Identifier id;

    private ConfigClassHandler(Class<T> type) {
        this.type = type;
    }

    public static <T> Builder<T> createBuilder(Class<T> type) {
        return new Builder<>(type);
    }

    /** Created on first use, not at build(): the settings classes assign static defaults after their HANDLER field. */
    public synchronized T instance() {
        if (instance == null) {
            try {
                var c = type.getDeclaredConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot create defaults for " + type, e);
            }
        }
        return instance;
    }

    public Identifier id() {
        return id;
    }

    public boolean load() {
        return true;
    }

    public void save() {}

    public static final class Builder<T> {
        private final ConfigClassHandler<T> handler;

        private Builder(Class<T> type) {
            this.handler = new ConfigClassHandler<>(type);
        }

        public Builder<T> id(Identifier id) {
            handler.id = id;
            return this;
        }

        public Builder<T> serializer(Function<ConfigClassHandler<T>, Object> serializer) {
            return this;
        }

        public ConfigClassHandler<T> build() {
            return handler;
        }
    }
}
