package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.StormsClientConfig;
import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import com.paulorchard.islandcraft.weatherofarrakis.storms.registry.ISEnvironmentAttributes;
import com.paulorchard.islandcraft.weatherofarrakis.storms.client.world.WeatherArchetype;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Prompt 04 (our code): the continuous hurricane wind. One or two looping head-relative instances in the WEATHER category (so the game's weather slider applies)
 * whose volume and pitch follow the storm strength (StormInput) and an outdoor factor from the sky light at the camera. Started and stopped by the storm itself:
 * when the sandstorm archetype is not active the volume fades to zero and the instances are halted.
 */
@EventBusSubscriber(modid = WeatherOfArrakis.MOD_ID, value = Dist.CLIENT)
public final class StormWindBed {
    private static StormSoundInstance bed, hiss;
    private static String bedId = "", hissId = "";
    private static Identifier dimension;
    private static float outdoor = 1f;
    public static float bedVolume, hissVolume, bedPitch, intensity;
    public static boolean active;

    private StormWindBed() {}

    /** 0 = deep indoors, 1 = open sky; smoothed over about one second. */
    public static float outdoor() {
        return outdoor;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            haltAll();
            return;
        }
        Identifier dim = mc.level.dimension().identifier();
        if (dimension != null && !dimension.equals(dim)) haltAll();
        dimension = dim;
        if (ModList.get().isLoaded("immersive_storms")) return;

        // outdoor factor: sky light at the eye, smoothed (a door or a cave mouth takes about a second)
        int sky = mc.level.getBrightness(LightLayer.SKY, BlockPos.containing(mc.player.getEyePosition()));
        outdoor += (Mth.clamp((sky - 3) / 10f, 0f, 1f) - outdoor) * 0.12f;

        float rain = StormInput.rain(mc.level, 1f), thunder = StormInput.thunder(mc.level, 1f);
        boolean sandstorm;
        try {
            sandstorm = mc.gameRenderer.getMainCamera().attributeProbe().getValue(ISEnvironmentAttributes.WEATHER_ARCHETYPE, 1f) == WeatherArchetype.SANDSTORM;
        } catch (RuntimeException ex) {
            sandstorm = false;
        }
        active = StormsClientConfig.WIND_BED_ENABLED.get() && sandstorm && rain > 0.02f;
        intensity = Mth.clamp(0.1f + 0.6f * rain + 0.3f * thunder, 0f, 1f) * (active ? 1f : 0f);

        float io = Mth.lerp(outdoor, StormsClientConfig.WIND_INDOOR_VOLUME.get().floatValue(), 1f);
        float pitchIndoor = Mth.lerp(outdoor, 0.8f, 1f);
        float basePitch = StormsClientConfig.WIND_BED_PITCH.get().floatValue() * (1f - 0.15f * thunder - 0.05f * com.paulorchard.islandcraft.weatherofarrakis.StormOverride.extra) * pitchIndoor;

        bedVolume = Mth.clamp(StormsClientConfig.WIND_BED_VOLUME.get().floatValue() * intensity * io, 0f, 1f);
        bedPitch = basePitch;
        // the hiss rises with thunder and dies faster indoors (it is the high band)
        hissVolume = Mth.clamp(StormsClientConfig.WIND_HISS_VOLUME.get().floatValue() * (0.5f * rain + 0.5f * thunder) * (active ? 1f : 0f) * io * io, 0f, 1f);

        bed = drive(bed, bedId, StormsClientConfig.WIND_BED_SOUND.get(), bedVolume, bedPitch, active, id -> bedId = id);
        hiss = drive(hiss, hissId, StormsClientConfig.WIND_HISS_SOUND.get(), hissVolume, Mth.clamp(basePitch * 1.05f, 0.5f, 2f), active, id -> hissId = id);
    }

    private interface IdSetter {
        void set(String id);
    }

    private static StormSoundInstance drive(StormSoundInstance cur, String curId, String wantId, float vol, float pitch, boolean on, IdSetter setId) {
        Minecraft mc = Minecraft.getInstance();
        if (cur != null && (cur.isStopped() || !curId.equals(wantId))) {
            cur.halt();
            cur = null;
        }
        if (cur == null) {
            if (!on || wantId.isBlank() || vol <= 0.001f) return null;
            Identifier ident = Identifier.tryParse(wantId);
            if (ident == null) return null;
            cur = new StormSoundInstance(SoundEvent.createVariableRangeEvent(ident), 0.001f, pitch, true);
            mc.getSoundManager().play(cur);
            setId.set(wantId);
            WeatherOfArrakis.LOGGER.info("[storm-sound] wind loop started: {}", wantId);
        }
        cur.targetVolume = vol;
        cur.targetPitch = pitch;
        if (!on && vol < 0.004f) {
            cur.halt();
            WeatherOfArrakis.LOGGER.info("[storm-sound] wind loop stopped: {}", wantId);
            return null;
        }
        return cur;
    }

    public static void haltAll() {
        if (bed != null) bed.halt();
        if (hiss != null) hiss.halt();
        bed = hiss = null;
        bedVolume = hissVolume = intensity = 0f;
        active = false;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        haltAll();
        dimension = null;
    }

    public static String report() {
        return String.format(java.util.Locale.ROOT, "wind: active=%s intensity=%.2f outdoor=%.2f bed=%s vol=%.2f pitch=%.2f hissVol=%.2f loopsPlaying=%d",
                active, intensity, outdoor, bed == null ? "-" : bedId, bedVolume, bedPitch, hissVolume, (bed == null ? 0 : 1) + (hiss == null ? 0 : 1));
    }
}
