package com.awesomehippo.chamelemobs.network;

import com.awesomehippo.chamelemobs.Chamelemobs;
import com.awesomehippo.chamelemobs.client.ClientEvents;
import com.awesomehippo.chamelemobs.config.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Chamelemobs.MOD_ID)
public class Network {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Chamelemobs.MOD_ID, "main"), () -> PROTOCOL, NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

    public static void register() {
        SimpleChannel.MessageBuilder<SyncPacket> builder = CHANNEL.messageBuilder(SyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPacket::encode)
                .decoder(SyncPacket::decode);
        if (FMLEnvironment.dist.isClient()) {
            builder.consumerMainThread((msg, ctx) -> ClientEvents.applyServerConfig(msg.enabled, msg.strength, msg.brightness, msg.mobs));
        } else {
            builder.consumerMainThread((msg, ctx) -> {});
        }
        builder.add();
    }

    public static void sendTo(ServerPlayer player) {
        if (!CHANNEL.isRemotePresent(player.connection.connection)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncPacket.current());
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

    public static class SyncPacket {
        public final boolean enabled;
        public final float strength;
        public final float brightness;
        public final List<String> mobs;

        private SyncPacket(boolean enabled, float strength, float brightness, List<String> mobs) {
            this.enabled = enabled;
            this.strength = strength;
            this.brightness = brightness;
            this.mobs = mobs;
        }

        private static SyncPacket current() {
            return new SyncPacket(Config.enabled, Config.strength, Config.brightness, new ArrayList<>(Config.mobIds()));
        }

        private static void encode(SyncPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.enabled);
            buf.writeFloat(packet.strength);
            buf.writeFloat(packet.brightness);
            buf.writeVarInt(packet.mobs.size());
            for (String id : packet.mobs) {
                buf.writeUtf(id);
            }
        }

        private static SyncPacket decode(FriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            float strength = buf.readFloat();
            float brightness = buf.readFloat();
            int n = buf.readVarInt();
            List<String> mobs = new ArrayList<>();
            if (n > 0 && n <= 512) {
                for (int i = 0; i < n; i++) {
                    mobs.add(buf.readUtf());
                }
            }
            return new SyncPacket(enabled, strength, brightness, mobs);
        }
    }
}
