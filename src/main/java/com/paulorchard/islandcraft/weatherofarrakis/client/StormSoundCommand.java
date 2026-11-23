package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Prompt 04 (our code): /storm_sound <soundEventId> [pitch] [volume] [loop] plays any sound event in the current world (weather category, not positional); /storm_sound stop. */
@EventBusSubscriber(modid = WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
public final class StormSoundCommand {
    private static final List<StormSoundInstance> PLAYING = new ArrayList<>();

    private StormSoundCommand() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent e) {
        var base = Commands.literal("storm_sound");
        base.then(Commands.literal("stop").executes(c -> stopAll(c.getSource())));
        base.then(Commands.argument("id", net.minecraft.commands.arguments.IdentifierArgument.id())
                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.keySet(), b))
                .executes(c -> play(c.getSource(), net.minecraft.commands.arguments.IdentifierArgument.getId(c, "id").toString(), 1f, 1f, false))
                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.1f, 4f))
                        .executes(c -> play(c.getSource(), net.minecraft.commands.arguments.IdentifierArgument.getId(c, "id").toString(), FloatArgumentType.getFloat(c, "pitch"), 1f, false))
                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0f, 4f))
                                .executes(c -> play(c.getSource(), net.minecraft.commands.arguments.IdentifierArgument.getId(c, "id").toString(), FloatArgumentType.getFloat(c, "pitch"), FloatArgumentType.getFloat(c, "volume"), false))
                                .then(Commands.argument("loop", BoolArgumentType.bool())
                                        .executes(c -> play(c.getSource(), net.minecraft.commands.arguments.IdentifierArgument.getId(c, "id").toString(), FloatArgumentType.getFloat(c, "pitch"), FloatArgumentType.getFloat(c, "volume"), BoolArgumentType.getBool(c, "loop")))))));
        e.getDispatcher().register(base);
    }

    private static int play(CommandSourceStack src, String id, float pitch, float volume, boolean loop) {
        Identifier ident = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (ident == null) {
            src.sendFailure(Component.literal("Not a valid id: " + id));
            return 0;
        }
        Minecraft mc = Minecraft.getInstance();
        var inst = new StormSoundInstance(SoundEvent.createVariableRangeEvent(ident), volume, pitch, loop);
        var result = mc.getSoundManager().play(inst);
        if (inst.getSound() == null || inst.getSound().getLocation().getPath().equals("empty")) {
            src.sendFailure(Component.literal("No such sound event (or it has no sounds): " + ident));
            return 0;
        }
        PLAYING.add(inst);
        WeatherOfArrakis.LOGGER.info("[storm-sound] playing {} pitch={} volume={} loop={} file={} result={} active={}", ident, pitch, volume, loop, inst.getSound().getLocation(), result, mc.getSoundManager().isActive(inst));
        src.sendSuccess(() -> Component.literal("Playing " + ident + " (pitch " + pitch + ", volume " + volume + (loop ? ", looping; /storm_sound stop" : "") + ")"), false);
        return 1;
    }

    private static int stopAll(CommandSourceStack src) {
        int n = PLAYING.size();
        stopAllQuiet();
        src.sendSuccess(() -> Component.literal("Stopped " + n + " sound(s)."), false);
        return 1;
    }

    public static void stopAllQuiet() {
        for (var i : PLAYING) i.halt();
        PLAYING.clear();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        stopAllQuiet();
    }
}
