package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Prompt 04 (our code): /storm_tune lists the sandstorm client settings, /storm_tune name value changes one live and saves the file. */
@EventBusSubscriber(modid = com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
public final class StormTuneCommand {
    private StormTuneCommand() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("storm_tune")
                .executes(c -> {
                    for (var en : StormsClientConfig.TUNABLE.entrySet()) {
                        String line = en.getKey() + " = " + en.getValue().get();
                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return 1;
                })
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(StormsClientConfig.TUNABLE.keySet(), b))
                        .then(Commands.argument("value", StringArgumentType.greedyString()).executes(c -> {
                            String name = StringArgumentType.getString(c, "name");
                            String raw = StringArgumentType.getString(c, "value").trim();
                            return set(c.getSource(), name, raw);
                        }))));
    }

    @SuppressWarnings("unchecked")
    private static int set(net.minecraft.commands.CommandSourceStack src, String name, String raw) {
        ModConfigSpec.ConfigValue<?> v = StormsClientConfig.TUNABLE.get(name);
        if (v == null) {
            src.sendFailure(Component.literal("Unknown setting. /storm_tune lists them."));
            return 0;
        }
        try {
            Object cur = v.get();
            if (cur instanceof Double) ((ModConfigSpec.ConfigValue<Double>) v).set(Double.parseDouble(raw));
            else if (cur instanceof Integer) ((ModConfigSpec.ConfigValue<Integer>) v).set(Integer.parseInt(raw));
            else if (cur instanceof Boolean) ((ModConfigSpec.ConfigValue<Boolean>) v).set(Boolean.parseBoolean(raw));
            else ((ModConfigSpec.ConfigValue<String>) v).set(raw);
            v.save();
        } catch (RuntimeException ex) {
            src.sendFailure(Component.literal("Could not set " + name + ": " + ex.getMessage()));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(name + " = " + v.get() + " (saved)"), false);
        return 1;
    }
}
