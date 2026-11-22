// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.network.ConfigSyncPacketPayload;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISAttributeTypes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISEnvironmentAttributes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISParticleTypes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISSoundEvents;
import com.paulorchard.islandcraft.weatherofarrakis.storms.util.ISCommandRegistrationCallback;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event.EventManager;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.YumiMods;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static net.minecraft.commands.Commands.literal;

public class ImmersiveStorms {
    public static final String MOD_NAMESPACE = "islandcraft_weatherofarrakis";
    public static final String MOD_ID = "islandcraft_weatherofarrakis";
    public static final EventManager<Identifier> EVENT_MANAGER = new EventManager<>(id("default"), Identifier::parse);
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAMESPACE);

    public void onInitialize() {
        ISParticleTypes.initialize();
        ISSoundEvents.initialize();
        ISAttributeTypes.initialize();
        ISEnvironmentAttributes.initialize();

        ISCommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            var reloadConfig = literal("reload")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                    .executes(ImmersiveStorms::reloadConfig);

            dispatcher.register(literal(MOD_ID).then(reloadConfig));
        });
    }

    public static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        if (AdvancedSettings.HANDLER.load()) {
            AdvancedSettings data = AdvancedSettings.HANDLER.instance();

            level.islandcraft_weatherofarrakis$rebindAttributeMap(data);

            ConfigSyncPacketPayload.SEND_TO_CLIENTS.invoker().sendToClients(source.getServer(), new ConfigSyncPacketPayload(data));

            source.sendSuccess(() -> Component.literal("Reloaded Immersive Storms config"), true);
        } else {
            source.sendFailure(Component.literal("Failed to reload Immersive Storms config"));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_NAMESPACE, path);
    }

    public static Path getConfigDir() {
        return YumiMods.get().getConfigDirectory().resolve(MOD_NAMESPACE);
    }
}