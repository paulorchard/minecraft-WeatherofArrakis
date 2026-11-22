package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders what the server said: fog (ViewportEvent), a dimming/tinting overlay (GUI layer under the HUD), blowing sand (particles), wind (looping sound).
 * Outdoor-ness comes from the client's own sky light at the camera, smoothed, so caves and closed rooms keep their vanilla look.
 */
@EventBusSubscriber(modid = WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
public final class WeatherClientEvents {
    /** 0 = deep inside, 1 = under open sky. Smoothed per frame. */
    private static float outdoor = 1f;
    private static long lastMs;
    private static WindLoop wind;

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
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (!ClientWeather.isActive()) return;
        ClientWeather.Look look = ClientWeather.current();
        float k = look.horizon() * outdoor;
        if (k <= 0.001f) return;
        float luma = Mth.clamp(0.2126f * e.getRed() + 0.7152f * e.getGreen() + 0.0722f * e.getBlue(), 0f, 1f);
        float bright = Mth.clamp(0.15f + luma * 1.1f, 0f, 1f); // keeps night dark: the sand colour is scaled by the vanilla daylight
        e.setRed(Mth.lerp(k, e.getRed(), 0.62f * bright));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.34f * bright));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.12f * bright));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        if (!ClientWeather.isActive() || e.getType() != FogType.ATMOSPHERIC) return;
        ClientWeather.Look look = ClientWeather.current();
        float k = outdoor;
        float target = look.fogBlocks();
        if (target >= ClientWeather.Look.NO_FOG - 1f || k <= 0.001f) return;
        var d = e.getFogData();
        // The storm look ignores the player's render distance fog limits: set the render-distance, sky and cloud ends too.
        float endNow = d.environmentalEnd;
        float end = Mth.lerp(k, endNow, Math.min(endNow, target));
        float start = Mth.lerp(k, d.environmentalStart, Math.min(d.environmentalStart, target * 0.1f));
        e.setFarPlaneDistance(end);
        e.setNearPlaneDistance(start);
        d.renderDistanceEnd = Math.min(d.renderDistanceEnd, Mth.lerp(k, d.renderDistanceEnd, end));
        d.renderDistanceStart = Math.min(d.renderDistanceStart, Mth.lerp(k, d.renderDistanceStart, start));
        d.skyEnd = Math.min(d.skyEnd, Mth.lerp(k, d.skyEnd, end));
        d.cloudEnd = Math.min(d.cloudEnd, Mth.lerp(k, d.cloudEnd, end));
    }

    @EventBusSubscriber(modid = WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent e) {
            // Directly above the camera overlays (so below crosshair, hotbar and the rest of the HUD).
            e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Identifier.fromNamespaceAndPath(WeatherOfArrakis.MOD_ID, "sand_overlay"), (g, delta) -> {
                if (!ClientWeather.isActive()) return;
                ClientWeather.Look l = ClientWeather.current();
                float a = outdoorNow() * Mth.clamp(l.sky() * 0.12f + (1f - l.sun()) * 0.60f + l.overlay() * 0.25f, 0f, 0.78f);
                if (a < 0.01f) return;
                Minecraft mc = Minecraft.getInstance();
                int alpha = (int) (a * 255) & 0xFF;
                int rgb = 0x3A1E08; // warm dark brown: dims the light and tints sky and sun
                g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), (alpha << 24) | rgb);
            });
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientWeather.reset();
        wind = null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        ClientWeather.Look look = ClientWeather.current();
        float out = outdoorNow();

        // wind bed: one looping sound whose volume follows the look; quieter indoors and underground
        float vol = look.wind() * (0.12f + 0.88f * out);
        if (vol > 0.005f) {
            if (wind == null || wind.isStopped()) {
                wind = new WindLoop();
                mc.getSoundManager().play(wind);
            }
            wind.setTarget(vol);
        } else if (wind != null) {
            wind.setTarget(0f);
            if (wind.volumeNow() <= 0.001f) {
                wind.finish();
                wind = null;
            }
        }

        // blowing sand, driven from the wind direction, outdoors only
        float rate = look.particles() * out;
        if (rate > 0.01f && !mc.isPaused()) {
            int count = Mth.floor(rate * 14 + level.getRandom().nextFloat());
            BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
            double wx = ClientWeather.windX(), wz = ClientWeather.windZ();
            for (int i = 0; i < count; i++) {
                double ox = (level.getRandom().nextDouble() - 0.5) * 24 - wx * 10;
                double oz = (level.getRandom().nextDouble() - 0.5) * 24 - wz * 10;
                double oy = level.getRandom().nextDouble() * 7 - 1.5;
                double speed = 0.35 + 0.35 * look.wind();
                level.addParticle(sand, player.getX() + ox, player.getY() + oy, player.getZ() + oz, wx * speed, 0.02, wz * speed);
            }
        }
    }
}
