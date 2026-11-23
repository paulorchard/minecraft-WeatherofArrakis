package com.paulorchard.islandcraft.weatherofarrakis.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherConfig;
import com.paulorchard.islandcraft.weatherofarrakis.core.LevelWeather;
import com.paulorchard.islandcraft.weatherofarrakis.core.Params;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Commands, permission level 2 (also from the console). Roots: coriolis and sandstorm. */
public final class WeatherCommands {
    private WeatherCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("coriolis").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("start")
                        .executes(c -> start(c.getSource(), WeatherConfig.APPROACH_SECONDS.get(), -1))
                        .then(Commands.argument("approachSeconds", IntegerArgumentType.integer(1, 36000))
                                .executes(c -> start(c.getSource(), IntegerArgumentType.getInteger(c, "approachSeconds"), -1))
                                .then(Commands.argument("stormSeconds", IntegerArgumentType.integer(1, 36000))
                                        .executes(c -> start(c.getSource(), IntegerArgumentType.getInteger(c, "approachSeconds"), IntegerArgumentType.getInteger(c, "stormSeconds"))))))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("strike")
                        .executes(c -> strike(c.getSource(), null, null))
                        .then(Commands.argument("x", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg()).then(Commands.argument("z", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                .executes(c -> strike(c.getSource(), com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "x"), com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "z"))))))
                .then(Commands.literal("major")
                        .then(Commands.literal("cancel").executes(c -> MajorCommands.cancel(c.getSource())))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 10000))
                                .executes(c -> MajorCommands.start(c.getSource(), IntegerArgumentType.getInteger(c, "minutes"), -1, -1))
                                .then(Commands.argument("stormSeconds", IntegerArgumentType.integer(1, 36000))
                                        .executes(c -> MajorCommands.start(c.getSource(), IntegerArgumentType.getInteger(c, "minutes"), IntegerArgumentType.getInteger(c, "stormSeconds"), -1))
                                        .then(Commands.argument("resetWindowSeconds", IntegerArgumentType.integer(1, 36000))
                                                .executes(c -> MajorCommands.start(c.getSource(), IntegerArgumentType.getInteger(c, "minutes"), IntegerArgumentType.getInteger(c, "stormSeconds"), IntegerArgumentType.getInteger(c, "resetWindowSeconds")))))))
                .then(Commands.literal("chunks")
                        .executes(c -> MajorCommands.chunks(c.getSource(), null))
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer()).then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                .executes(c -> MajorCommands.chunks(c.getSource(), new net.minecraft.world.level.ChunkPos(IntegerArgumentType.getInteger(c, "chunkX"), IntegerArgumentType.getInteger(c, "chunkZ")))))))
                .then(Commands.literal("reset")
                        .then(Commands.literal("all").executes(c -> MajorCommands.resetAll(c.getSource())))
                        .then(Commands.literal("here").executes(c -> MajorCommands.resetHere(c.getSource()))))
                .then(Commands.literal("mark")
                        .executes(c -> MajorCommands.mark(c.getSource(), null))
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer()).then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                .executes(c -> MajorCommands.mark(c.getSource(), new net.minecraft.world.level.ChunkPos(IntegerArgumentType.getInteger(c, "chunkX"), IntegerArgumentType.getInteger(c, "chunkZ"))))))));
        d.register(Commands.literal("sandstorm").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start")
                        .executes(c -> sandStart(c.getSource(), 1, false))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 600))
                                .executes(c -> sandStart(c.getSource(), IntegerArgumentType.getInteger(c, "minutes"), false))
                                .then(Commands.literal("thunder").executes(c -> sandStart(c.getSource(), IntegerArgumentType.getInteger(c, "minutes"), true))))
                        .then(Commands.literal("thunder").executes(c -> sandStart(c.getSource(), 1, true))))
                .then(Commands.literal("stop").executes(c -> sandStop(c.getSource()))));
    }

    private static Component text(String s) {
        return Component.literal(s);
    }

    private static int status(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (!WeatherConfig.isQualifying(level)) {
            src.sendSuccess(() -> text("Weather of Arrakis is inactive in " + level.dimension().identifier() + " (not in the dimensions list)."), false);
            return 0;
        }
        LevelWeather w = WeatherManager.state(level);
        Params p = WeatherConfig.params();
        StringBuilder sb = new StringBuilder();
        sb.append("Coriolis: ").append(w.phase);
        if (w.phase.isActiveStorm()) sb.append(" (").append((w.phaseTicksLeft() + 19) / 20).append(" s left").append(w.major ? ", Major" : "").append(')');
        long clock = level.getOverworldClockTime();
        if (w.scheduledAt >= 0 && w.phase == com.paulorchard.islandcraft.weatherofarrakis.core.Phase.CALM) {
            double days = (w.scheduledAt - clock) / (double) Params.DAY_TICKS;
            sb.append("; next storm in ").append(String.format(Locale.ROOT, "%.2f", days)).append(" game days");
        } else if (w.scheduledAt >= 0) {
            sb.append("; next ordinary storm scheduled in ").append(String.format(Locale.ROOT, "%.2f", (w.scheduledAt - clock) / (double) Params.DAY_TICKS)).append(" game days");
        }
        sb.append(". Sandstorm: ");
        if (w.sandActive) sb.append("active, ").append(String.format(Locale.ROOT, "%.0f", w.sandTicksLeft / 1200.0 * 60)).append(" s left, intensity ").append(String.format(Locale.ROOT, "%.2f", w.sandstormIntensity(p.sandRampTicks())));
        else sb.append("none").append(w.sandNextTicks >= 0 ? ", next in " + (w.sandNextTicks / 1200) + " min" : "");
        String out = sb.toString();
        src.sendSuccess(() -> text(out), false);
        return 1;
    }

    private static int start(CommandSourceStack src, int approach, int storm) {
        ServerLevel level = src.getLevel();
        if (level.getServer().overworld().getWeatherData().isRaining() || level.getWeatherData().isRaining()) {
            src.sendFailure(text("A sandstorm (vanilla rain) is running: /sandstorm stop first. A Coriolis storm and a sandstorm do not run together."));
            return 0;
        }
        int stormSeconds = storm > 0 ? storm : WeatherConfig.STORM_SECONDS_MIN.get() + level.getRandom().nextInt(Math.max(1, WeatherConfig.STORM_SECONDS_MAX.get() - WeatherConfig.STORM_SECONDS_MIN.get() + 1));
        if (!WeatherManager.startCoriolis(level, approach, stormSeconds)) {
            src.sendFailure(text(WeatherConfig.isQualifying(level) ? "A Coriolis storm is already running here." : "This dimension is not in the weather dimensions list."));
            return 0;
        }
        src.sendSuccess(() -> text("Coriolis storm started: approach " + approach + " s, storm " + stormSeconds + " s (a test storm never resets chunks)."), true);
        return 1;
    }

    private static int strike(CommandSourceStack src, Double x, Double z) {
        ServerLevel level = src.getLevel();
        double sx = x != null ? x : src.getPosition().x + 20 + level.getRandom().nextInt(20);
        double sz = z != null ? z : src.getPosition().z + level.getRandom().nextInt(20) - 10;
        Lightning.strike(level, sx, sz);
        src.sendSuccess(() -> text(String.format(Locale.ROOT, "Lightning strike at %.0f %.0f.", sx, sz)), true);
        return 1;
    }

    private static int stop(CommandSourceStack src) {
        WeatherManager.stopCoriolis(src.getLevel());
        src.sendSuccess(() -> text("Coriolis storm stopped."), true);
        return 1;
    }

    private static int sandStart(CommandSourceStack src, int minutes, boolean thunder) {
        var level = src.getLevel();
        if (WeatherConfig.isQualifying(level) && WeatherManager.state(level).phase != com.paulorchard.islandcraft.weatherofarrakis.core.Phase.CALM) {
            src.sendFailure(text("A Coriolis storm is active here (" + WeatherManager.state(level).phase + "): /coriolis stop first. A sandstorm and a Coriolis storm do not run together."));
            return 0;
        }
        if (!WeatherManager.startSandstorm(level, minutes, thunder)) {
            src.sendFailure(text("Cannot start a sandstorm here: this dimension is not in the dimensions config."));
            return 0;
        }
        var wd = level.getServer().getWorldData();
        src.sendSuccess(() -> text("Sandstorm (vanilla rain" + (thunder ? " and thunder" : "") + ") set for " + minutes + " minute(s); it shows in every biome that has rain (the Nether and End have no weather). Rain gradient now " + String.format(Locale.ROOT, "%.2f", level.getRainLevel(1f))), true);
        return 1;
    }

    private static int sandStop(CommandSourceStack src) {
        WeatherManager.stopSandstorm(src.getLevel());
        src.sendSuccess(() -> text("Sandstorm stopped."), true);
        return 1;
    }
}
