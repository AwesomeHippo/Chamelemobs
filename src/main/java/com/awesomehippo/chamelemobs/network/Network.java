package com.awesomehippo.chamelemobs.network;

import com.awesomehippo.chamelemobs.Chamelemobs;
import com.awesomehippo.chamelemobs.client.ClientEvents;
import com.awesomehippo.chamelemobs.config.Config;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Chamelemobs.MOD_ID)
public class Network {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(SyncPacket.TYPE, SyncPacket.STREAM_CODEC, (msg, ctx) -> ClientEvents.applyServerConfig(msg.enabled, msg.strength, msg.mobs));
        } else {
            registrar.playToClient(SyncPacket.TYPE, SyncPacket.STREAM_CODEC, (msg, ctx) -> {});
        }
    }

    public static void sendTo(ServerPlayer player) {
        if (!player.connection.hasChannel(SyncPacket.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, SyncPacket.current());
    }

    public static void sendToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    public static class SyncPacket implements CustomPacketPayload {
        public static final Type<SyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Chamelemobs.MOD_ID, "main"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPacket> STREAM_CODEC = StreamCodec.of(SyncPacket::encode, SyncPacket::decode);

        public final boolean enabled;
        public final float strength;
        public final List<String> mobs;

        private SyncPacket(boolean enabled, float strength, List<String> mobs) {
            this.enabled = enabled;
            this.strength = strength;
            this.mobs = mobs;
        }

        private static SyncPacket current() {
            return new SyncPacket(Config.enabled, Config.strength, new ArrayList<>(Config.mobIds()));
        }

        private static void encode(RegistryFriendlyByteBuf buf, SyncPacket packet) {
            buf.writeBoolean(packet.enabled);
            buf.writeFloat(packet.strength);
            buf.writeVarInt(packet.mobs.size());
            for (String id : packet.mobs) {
                buf.writeUtf(id);
            }
        }

        private static SyncPacket decode(RegistryFriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            float strength = buf.readFloat();
            int n = buf.readVarInt();
            List<String> mobs = new ArrayList<>();
            if (n > 0 && n <= 512) {
                for (int i = 0; i < n; i++) {
                    mobs.add(buf.readUtf());
                }
            }
            return new SyncPacket(enabled, strength, mobs);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
