package com.paulorchard.islandcraft.weatherofarrakis.client.immersive;

import java.lang.reflect.Method;

/** Iris shader pack in use? By reflection (soft dependency); false when Iris is absent or its API changed. */
public final class ShaderDetect {
    private static Method inUse;
    private static Object api;
    private static boolean tried;

    private ShaderDetect() {}

    public static boolean active() {
        if (!tried) {
            tried = true;
            try {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse");
            } catch (Throwable t) {
                inUse = null;
            }
        }
        if (inUse == null) return false;
        try {
            return (boolean) inUse.invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }
}
