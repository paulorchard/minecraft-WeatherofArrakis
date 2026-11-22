package com.paulorchard.islandcraft.weatherofarrakis.claims;

import java.lang.reflect.Method;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Open Parties and Claims via reflection (written from its published Javadoc; not tested, the mod is not in the test instance).
 * OpenPACServerAPI.get(server).getServerClaimsManager().get(dimension, chunkPos) returns a claim, or null for wilderness.
 * The dimension parameter type is not checked at build time; if the call fails the registry fails closed.
 */
final class OpenPartiesProvider implements ChunkProtection {
    private final Method getApi;
    private final Method getManager;
    private final Method get;

    OpenPartiesProvider() throws Exception {
        Class<?> api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
        this.getApi = api.getMethod("get", MinecraftServer.class);
        this.getManager = api.getMethod("getServerClaimsManager");
        Method found = null;
        for (Method m : Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI").getMethods()) {
            if (m.getName().equals("get") && m.getParameterCount() == 2 && m.getParameterTypes()[1] == ChunkPos.class) found = m;
        }
        if (found == null) throw new NoSuchMethodException("IServerClaimsManagerAPI.get(dimension, ChunkPos)");
        this.get = found;
    }

    @Override
    public String id() {
        return "openpartiesandclaims";
    }

    @Override
    public boolean isProtected(ServerLevel level, ChunkPos pos) throws Exception {
        Object mgr = getManager.invoke(getApi.invoke(null, level.getServer()));
        return get.invoke(mgr, level.dimension().identifier(), pos) != null;
    }

    @Override
    public String describe(ServerLevel level, ChunkPos pos) {
        return "Open Parties and Claims";
    }
}
