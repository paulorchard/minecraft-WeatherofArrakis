// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config.section;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.ConfigClassHandler;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.SerialEntry;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl.GsonConfigSerializerBuilder;

import java.nio.file.Path;

public class SettingsSchema {
    public static final Path PATH = ImmersiveStorms.getConfigDir().resolve("schema.json5");

    public static final ConfigClassHandler<SettingsSchema> HANDLER = ConfigClassHandler.createBuilder(SettingsSchema.class)
            .id(ImmersiveStorms.id("schema"))
            .serializer(
                    config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(PATH)
                            .setJson5(true)
                            .build()
            )
            .build();

    public static final int CONFIG_VERSION = 3;

    @SerialEntry(comment = "Config schema version, do not touch! Changing this value may result in unexpected behaviour.")
    int schemaVersion = CONFIG_VERSION;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}