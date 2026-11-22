// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event.Event;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;

public interface ISCommandRegistrationCallback {
    Event<Identifier, ISCommandRegistrationCallback> EVENT = ImmersiveStorms.EVENT_MANAGER.create(
            ISCommandRegistrationCallback.class,
            listeners -> (dispatcher, buildContext, selection) -> {
                for (ISCommandRegistrationCallback listener : listeners) {
                    listener.register(dispatcher, buildContext, selection);
                }
            });

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection);
}
