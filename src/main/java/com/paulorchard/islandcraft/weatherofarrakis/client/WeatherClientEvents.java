package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Prompt 04: the old Coriolis look (staged fog, tint overlay, blowing sand, wind loop) is removed; the storm is shown by the copied Immersive Storms effects, fed
 * through ClientWeather and StormOverride. What stays here: camera shake, the warm fog cast of the storm itself, the storm front tint, and the debug hooks.
 * Outdoor-ness comes from the client's own sky light at the camera, smoothed, so caves and closed rooms keep their vanilla look.
 */
@EventBusSubscriber(modid = WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
public final class WeatherClientEvents {
    /** 0 = deep inside, 1 = under open sky. Smoothed per frame. */
    private static float outdoor = 1f;
    private static long lastMs;

    private WeatherClientEvents() {}

    /** Smoothed outdoor factor; updates from the camera's sky light. */
    private static float outdoorNow() {
        Minecraft mc = Minecraft.getInstance();
        long now = net.minecraft.util.Util.getMillis();
        float dt = Math.min(0.25f, (now - lastMs) / 1000f);
        lastMs = now;
        if (mc.level == null || mc.player == null) return outdoor;
        int sky = mc.level.getBrightness(LightLayer.SKY, BlockPos.containing(mc.player.getEyePosition()));
        float target = Mth.clamp((sky - 3) / 10f, 0f, 1f);
        outdoor += (target - outdoor) * Math.min(1f, dt * 2.5f);
        return outdoor;
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles e) {
        if (!com.paulorchard.islandcraft.weatherofarrakis.ClientConfig.SHAKE_ENABLED.get()) return;
        float amp = ShakeClient.amplitude();
        if (ClientWeather.isStorm() && ClientWeather.tremble() && outdoor > 0.5f) amp += 0.07f * outdoor;
        amp *= com.paulorchard.islandcraft.weatherofarrakis.ClientConfig.SHAKE_SCALE.get().floatValue();
        if (amp <= 0.001f) return;
        double t = net.minecraft.util.Util.getMillis() / 1000.0;
        float deg = 1.6f * amp;
        e.setYaw(e.getYaw() + deg * (float) (Math.sin(t * 41.0) * 0.6 + Math.sin(t * 23.0 + 1.3) * 0.4));
        e.setPitch(e.getPitch() + deg * (float) (Math.sin(t * 37.0 + 0.7) * 0.6 + Math.sin(t * 19.0) * 0.4));
        float roll = com.paulorchard.islandcraft.weatherofarrakis.ClientConfig.SHAKE_ROLL_DEGREES.get().floatValue();
        if (roll > 0f) e.setRoll(e.getRoll() + roll * amp * (float) Math.sin(t * 29.0));
    }

    /** The storm itself pushes the fog colour toward a warm red-brown (coriolis_storm_warm_cast); the sandstorm colour multiply comes from the copied attributes. */
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        ClientDebug.colorEvents++;
        if (!ClientWeather.isActive()) return;
        ClientWeather.Eff eff = ClientWeather.current();
        float luma = Mth.clamp(0.2126f * e.getRed() + 0.7152f * e.getGreen() + 0.0722f * e.getBlue(), 0f, 1f);
        float bright = Mth.clamp(0.15f + luma * 1.1f, 0f, 1f); // keeps night dark
        float k = eff.extra() * StormsClientConfig.CORIOLIS_STORM_WARM.get().floatValue() * outdoor;
        if (k > 0.001f) {
            ClientDebug.colorApplied++;
            e.setRed(Mth.lerp(k, e.getRed(), 0.62f * bright));
            e.setGreen(Mth.lerp(k, e.getGreen(), 0.34f * bright));
            e.setBlue(Mth.lerp(k, e.getBlue(), 0.12f * bright));
        }
        frontTint(e, bright);
    }

    /** Storm front, approach B (experimental, server switch storm_front): fog colour gets redder the more the camera looks upwind. */
    private static void frontTint(ViewportEvent.ComputeFogColor e, float bright) {
        if (!ClientWeather.front() || ClientWeather.kind() != 2) return;
        var f = e.getCamera().forwardVector();
        float dot = f.x() * -ClientWeather.windX() + f.z() * -ClientWeather.windZ();
        float k = Mth.clamp(dot, 0f, 1f) * ClientWeather.current().rain() * outdoor * 0.7f;
        if (k <= 0.001f) return;
        e.setRed(Mth.lerp(k, e.getRed(), 0.85f * bright));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.25f * bright));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.06f * bright));
    }

    /** Only records what the game ends up with; the fog itself is set by the copied StormFogModifier (prompt 04). */
    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        ClientDebug.fogEvents++;
        if (e.getType() == FogType.ATMOSPHERIC) ClientDebug.fogEndSeen = e.getFogData().environmentalEnd;
    }

    @SubscribeEvent
    public static void debugCommand(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("storm_debug").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            var p = ClientDebug.last;
            var eff = ClientWeather.current();
            String s = "payloads=" + ClientDebug.payloads + (p == null ? " (none received)" : " last: kind=" + p.kind() + " stage=" + p.stage() + " intensity=" + p.intensity() + " wind=(" + p.windX() + "," + p.windZ() + ") blend=" + p.blendSeconds() + "s received at game time " + ClientDebug.lastGameTime)
                    + "; dimension=" + (mc.level == null ? "none" : mc.level.dimension().identifier()) + "; outdoor=" + ClientDebug.outdoor
                    + "; effective rain=" + eff.rain() + " thunder=" + eff.thunder() + " extra=" + eff.extra()
                    + "; fog end=" + ClientDebug.fogEndSeen + "; " + StormWindBed.report();
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(s), false);
            return 1;
        }));
    }

    @SubscribeEvent
    public static void shotCommand(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) {
        var shot = net.minecraft.commands.Commands.literal("shot").executes(c -> {
            StormShot.request("storm-manual", 2f);
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Screenshot in 2 s."), false);
            return 1;
        }).then(net.minecraft.commands.Commands.argument("name", StringArgumentType.word()).executes(c -> {
            String name = StringArgumentType.getString(c, "name");
            StormShot.request(name, 1f);
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Screenshot " + name + ".png in 1 s."), false);
            return 1;
        }));
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("storm_debug").then(shot));
    }

    @SubscribeEvent
    public static void soundCount(net.neoforged.neoforge.client.event.sound.PlaySoundEvent e) {
        if (e.getName() != null && e.getName().contains("weather.")) ClientDebug.soundsPlayed++;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientWeather.reset();
        ShakeClient.clear();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        ClientDebug.outdoor = outdoorNow();
        ClientWeather.pushOverride();
        ClientDebug.sample();
        ClientDebug.tick(ClientWeather.isActive() || ClientDebug.rainGradient > 0.001f);
        StormShot.tick();
    }
}
