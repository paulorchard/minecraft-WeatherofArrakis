package com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl;

import com.google.gson.GsonBuilder;
import java.nio.file.Path;
import java.util.function.Function;

/** Stand-in for YACL's GsonConfigSerializerBuilder: accepts the same calls and does nothing (the config is in memory). Not adapted from upstream. */
public final class GsonConfigSerializerBuilder<T> {
    private GsonConfigSerializerBuilder() {}

    public static <T> GsonConfigSerializerBuilder<T> create(ConfigClassHandler<T> handler) {
        return new GsonConfigSerializerBuilder<>();
    }

    public GsonConfigSerializerBuilder<T> appendGsonBuilder(Function<GsonBuilder, GsonBuilder> fn) {
        return this;
    }

    public GsonConfigSerializerBuilder<T> setPath(Path path) {
        return this;
    }

    public GsonConfigSerializerBuilder<T> setJson5(boolean json5) {
        return this;
    }

    public Object build() {
        return this;
    }
}
