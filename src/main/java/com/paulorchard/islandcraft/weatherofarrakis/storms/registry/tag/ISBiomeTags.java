// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.registry.tag;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ISBiomeTags {
    public static final TagKey<Biome> HAS_SANDSTORMS = create("has_sandstorms");
    /** Added for IslandCraft (not upstream): a pseudo tag that UnboundRegistryObjectStream.Tag resolves to every biome. */
    public static final TagKey<Biome> ALL_BIOMES = create("all_biomes");
    public static final TagKey<Biome> HAS_BLIZZARDS = create("has_blizzards");
    public static final TagKey<Biome> HAS_DENSE_FOG = create("has_dense_fog");
    public static final TagKey<Biome> IS_WINDY = create("is_windy");
    public static final TagKey<Biome> IS_BLACK_RAIN_AFFECTED = create("is_black_rain_affected");

    private static TagKey<Biome> create(String id) {
        return TagKey.create(Registries.BIOME, ImmersiveStorms.id(id));
    }

    private ISBiomeTags() {

    }
}