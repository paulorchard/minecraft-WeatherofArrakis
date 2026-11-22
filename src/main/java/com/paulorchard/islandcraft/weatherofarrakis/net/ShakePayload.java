package com.paulorchard.islandcraft.weatherofarrakis.net;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A camera shake the server asks one client to play. strength 0..1 (already scaled by shelter or distance), seconds = length. */
public record ShakePayload(float strength, float seconds) implements CustomPacketPayload {
    public static final Type<ShakePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WeatherOfArrakis.MOD_ID, "shake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShakePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeFloat(p.strength);
                buf.writeFloat(p.seconds);
            },
            buf -> new ShakePayload(buf.readFloat(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
