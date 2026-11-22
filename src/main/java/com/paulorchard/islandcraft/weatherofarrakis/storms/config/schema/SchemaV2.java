// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config.schema;

import com.google.gson.JsonObject;
import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.GeneralSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.SandstormSettings;

import java.io.IOException;
import java.nio.file.Files;

public final class SchemaV2 {
    public static void run(int originalSchemaVersion) throws IOException {
        // load old files
        JsonObject sandstormConfig = JsonCopyHelper.read(SandstormSettings.PATH);
        JsonObject generalConfig = JsonCopyHelper.read(GeneralSettings.PATH);

        // apply updates
        JsonCopyHelper.copyBooleanProperty(sandstormConfig, generalConfig, "enableSandstormSounds");
        JsonCopyHelper.rename(generalConfig, "enableSandstormSounds", "enableStrongWindSounds");

        // save files
        Files.createDirectories(ImmersiveStorms.getConfigDir());
        Files.writeString(SandstormSettings.PATH, sandstormConfig.toString());
        Files.writeString(GeneralSettings.PATH, generalConfig.toString());
    }

    private SchemaV2() {

    }
}