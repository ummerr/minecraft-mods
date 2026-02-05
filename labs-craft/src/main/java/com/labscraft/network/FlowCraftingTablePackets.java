package com.labscraft.network;

import com.labscraft.LabsCraft;
import com.labscraft.screen.FlowCraftingTableScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class FlowCraftingTablePackets {

    public record CraftNanoBananaPayload() implements CustomPayload {
        public static final Id<CraftNanoBananaPayload> ID = new Id<>(Identifier.of(LabsCraft.MOD_ID, "craft_nano_banana"));
        public static final PacketCodec<RegistryByteBuf, CraftNanoBananaPayload> CODEC = PacketCodec.unit(new CraftNanoBananaPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CraftVeoPayload() implements CustomPayload {
        public static final Id<CraftVeoPayload> ID = new Id<>(Identifier.of(LabsCraft.MOD_ID, "craft_veo"));
        public static final PacketCodec<RegistryByteBuf, CraftVeoPayload> CODEC = PacketCodec.unit(new CraftVeoPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(CraftNanoBananaPayload.ID, CraftNanoBananaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CraftVeoPayload.ID, CraftVeoPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CraftNanoBananaPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof FlowCraftingTableScreenHandler handler) {
                    handler.onCraftNanoBanana();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CraftVeoPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof FlowCraftingTableScreenHandler handler) {
                    handler.onCraftVeo();
                }
            });
        });
    }

    public static void registerClient() {
        // Client doesn't need to receive these packets, only send them
    }

    public static void sendCraftNanoBananaPacket() {
        ClientPlayNetworking.send(new CraftNanoBananaPayload());
    }

    public static void sendCraftVeoPacket() {
        ClientPlayNetworking.send(new CraftVeoPayload());
    }
}
