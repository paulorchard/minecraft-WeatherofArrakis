// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config.schema;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;

import java.io.IOException;
import java.nio.file.Files;

public final class SchemaV3 {
    public static void run(int originalSchemaVersion) throws IOException {
        Files.delete(ImmersiveStorms.getConfigDir().resolve("biomes.json5"));
    }

    private SchemaV3() {

    }
}