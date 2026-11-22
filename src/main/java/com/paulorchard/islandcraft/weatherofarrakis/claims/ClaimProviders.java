package com.paulorchard.islandcraft.weatherofarrakis.claims;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;

/** Registry of chunk protection providers. With no claim mod installed nothing is protected. Fails closed. */
public final class ClaimProviders {
    private static final List<ChunkProtection> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Set<String> LOGGED = new HashSet<>();

    private ClaimProviders() {}

    /** Other mods (Subfiefs of Arrakis later) may register their own provider. */
    public static void register(ChunkProtection p) {
        PROVIDERS.add(p);
    }

    /** Builds the built-in adapters for the claim mods that are loaded. Each adapter touches its mod only through reflection. */
    public static void init() {
        tryBuild("ftbchunks", FtbChunksProvider::new);
        tryBuild("openpartiesandclaims", OpenPartiesProvider::new);
        String fake = System.getProperty("islandcraft.weather.fakeProtected"); // test aid: "x,z;x,z" chunks protected by a fake provider
        if (fake != null && !fake.isBlank()) register(new FakeProvider(fake));
    }

    private static void tryBuild(String modId, Callable<ChunkProtection> factory) {
        if (!ModList.get().isLoaded(modId)) return;
        try {
            register(factory.call());
            WeatherOfArrakis.LOGGER.info("Claim protection adapter active: {}", modId);
        } catch (Throwable t) {
            // Mod present but the adapter cannot be built: fail closed (every chunk protected) rather than risk a claim.
            WeatherOfArrakis.LOGGER.error("Claim mod {} is loaded but its adapter failed ({}); chunks will be treated as protected", modId, t.toString());
            register(new ChunkProtection() {
                @Override public String id() { return modId; }
                @Override public boolean isProtected(ServerLevel level, ChunkPos pos) { return true; }
                @Override public String describe(ServerLevel level, ChunkPos pos) { return modId + " (adapter failed, failing closed)"; }
            });
        }
    }

    private static boolean enabled(ChunkProtection p) {
        boolean builtIn = p.id().equals("ftbchunks") || p.id().equals("openpartiesandclaims");
        return !builtIn || WeatherConfig.CLAIM_PROVIDERS.get().contains(p.id());
    }

    /** Description from the first enabled provider that protects the chunk, or empty. A provider that throws protects the chunk (logged once). */
    public static Optional<String> protectedBy(ServerLevel level, ChunkPos pos) {
        for (ChunkProtection p : PROVIDERS) {
            if (!enabled(p)) continue;
            try {
                if (p.isProtected(level, pos)) return Optional.of(p.describe(level, pos));
            } catch (Throwable t) {
                if (LOGGED.add(p.id())) WeatherOfArrakis.LOGGER.error("Claim provider {} failed ({}); treating chunks as protected", p.id(), t.toString());
                return Optional.of(p.id() + " (error, failing closed)");
            }
        }
        return Optional.empty();
    }

    public static boolean isProtected(ServerLevel level, ChunkPos pos) {
        return protectedBy(level, pos).isPresent();
    }

    public static List<String> activeIds() {
        List<String> ids = new ArrayList<>();
        for (ChunkProtection p : PROVIDERS) ids.add(p.id());
        return ids;
    }

    /** Test aid only: chunks listed in the system property islandcraft.weather.fakeProtected are protected. */
    private static final class FakeProvider implements ChunkProtection {
        private final Set<Long> chunks = new HashSet<>();

        FakeProvider(String spec) {
            for (String s : spec.split(";")) {
                String[] p = s.trim().split(",");
                if (p.length == 2) chunks.add(ChunkPos.pack(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())));
            }
        }

        @Override public String id() { return "fake"; }
        @Override public boolean isProtected(ServerLevel level, ChunkPos pos) { return chunks.contains(pos.pack()); }
        @Override public String describe(ServerLevel level, ChunkPos pos) { return "fake test provider"; }
    }
}
