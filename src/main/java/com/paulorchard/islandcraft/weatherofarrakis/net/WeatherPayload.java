package com.paulorchard.islandcraft.weatherofarrakis.net;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The effective weather of the client's level, server to client. The client never decides; it blends to what it is told.
 *
 * @param kind        0 none, 1 sandstorm, 2 Coriolis approach (stage 1..6), 3 Coriolis storm
 * @param stage       approach stage 1..6 (kind 2), else 0
 * @param intensity   0..1 (sandstorm intensity target; 1 for Coriolis kinds)
 * @param windX       unit vector the wind blows towards
 * @param blendSeconds seconds the client takes to blend from what it shows to this
 * @param major       true for a Major Coriolis storm (cosmetic)
 * @param tremble     server setting: faint tremble while exposed in the storm
 * @param front       server setting: storm_front (directional fog tint during the approach)
 */
public record WeatherPayload(int kind, int stage, float intensity, float windX, float windZ, float blendSeconds, boolean major, boolean tremble, boolean front)
        implements CustomPacketPayload {
    public WeatherPayload(int kind, int stage, float intensity, float windX, float windZ, float blendSeconds, boolean major) {
        this(kind, stage, intensity, windX, windZ, blendSeconds, major, false, false);
    }

    public WeatherPayload withFlags(boolean tremble, boolean front) {
        return new WeatherPayload(kind, stage, intensity, windX, windZ, blendSeconds, major, tremble, front);
    }

    public static final int NONE = 0, SANDSTORM = 1, APPROACH = 2, STORM = 3;

    public static final Type<WeatherPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WeatherOfArrakis.MOD_ID, "weather"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                buf.writeByte(p.stage);
                buf.writeFloat(p.intensity);
                buf.writeFloat(p.windX);
                buf.writeFloat(p.windZ);
                buf.writeFloat(p.blendSeconds);
                buf.writeBoolean(p.major);
                buf.writeBoolean(p.tremble);
                buf.writeBoolean(p.front);
            },
            buf -> new WeatherPayload(buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
