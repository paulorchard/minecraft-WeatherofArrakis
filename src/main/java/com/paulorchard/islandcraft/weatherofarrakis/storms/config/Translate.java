// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config;

import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.ConfigClassHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class Translate {
    public static String prefixKey(ConfigClassHandler<?> handler) {
        return "yacl3.config." + handler.id().toString();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface NoComment {

    }

    private Translate() {

    }
}
