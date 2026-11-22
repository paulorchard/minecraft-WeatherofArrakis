// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.config.schema;

import java.io.IOException;

public interface ConfigUpdater {
    void run(int originalSchemaVersion) throws IOException;
}