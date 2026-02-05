package com.labscraft.network;

import com.labscraft.LabsCraft;
import com.labscraft.screen.VeoConsoleScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class VeoConsolePackets {

    public record GeneratePayload() implements CustomPayload {
        public static final Id<GeneratePayload> ID = new Id<>(Identifier.of(LabsCraft.MOD_ID, "veo_generate"));
        public static final PacketCodec<RegistryByteBuf, GeneratePayload> CODEC = PacketCodec.unit(new GeneratePayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(GeneratePayload.ID, GeneratePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GeneratePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof VeoConsoleScreenHandler handler) {
                    handler.onGenerateButtonClicked(context.player());
                }
            });
        });
    }

    public static void registerClient() {
        // Client doesn't need to receive this packet, only send it
    }

    public static void sendGeneratePacket() {
        ClientPlayNetworking.send(new GeneratePayload());
    }
}
