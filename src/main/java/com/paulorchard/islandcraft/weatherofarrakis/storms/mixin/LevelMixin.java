// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.mixin;

import com.paulorchard.islandcraft.weatherofarrakis.storms.world.AttributeMapHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin implements AttributeMapHolder {
}