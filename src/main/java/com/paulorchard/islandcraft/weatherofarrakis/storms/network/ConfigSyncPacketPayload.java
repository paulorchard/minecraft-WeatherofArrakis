// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md
package com.paulorchard.islandcraft.weatherofarrakis.storms.network;

import com.paulorchard.islandcraft.weatherofarrakis.storms.ImmersiveStorms;
import com.paulorchard.islandcraft.weatherofarrakis.storms.config.section.AdvancedSettings;
import com.paulorchard.islandcraft.weatherofarrakis.storms.shim.event.Event;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;

public record ConfigSyncPacketPayload(AdvancedSettings settings) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigSyncPacketPayload> TYPE = new CustomPacketPayload.Type<>(
            ImmersiveStorms.id("config_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacketPayload> STREAM_CODEC = StreamCodec.composite(
            AdvancedSettings.STREAM_CODEC,
            p -> p.settings,
            ConfigSyncPacketPayload::new
    );

    public static final Event<Identifier, SendToClients> SEND_TO_CLIENTS = ImmersiveStorms.EVENT_MANAGER.create(
            SendToClients.class,
            listeners -> (server, packet) -> {
                for (SendToClients listener : listeners) {
                    listener.sendToClients(server, packet);
                }
            }
    );

    public ConfigSyncPacketPayload() {
        this(AdvancedSettings.HANDLER.instance());
    }

    public static Packet<ClientCommonPacketListener> createClientboundPacket() {
        return new ClientboundCustomPayloadPacket(new ConfigSyncPacketPayload());
    }

    @Override
    @NonNull
    public Type<ConfigSyncPacketPayload> type() {
        return TYPE;
    }

    @FunctionalInterface
    public interface SendToClients {
        void sendToClients(MinecraftServer server, ConfigSyncPacketPayload packet);
    }
}