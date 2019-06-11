package alexiil.mc.lib.net.impl;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import alexiil.mc.lib.net.EnumNetSide;
import alexiil.mc.lib.net.NetByteBuf;

/** A connection on the server side to a specific {@link ServerPlayerEntity}. */
public class ActiveServerConnection extends ActiveMinecraftConnection {
    public final ServerPlayNetworkHandler netHandler;

    public ActiveServerConnection(ServerPlayNetworkHandler netHandler) {
        // PacketContext is implemented through the mixin MixinServerPlayNetworkHandler
        super((PacketContext) netHandler);
        this.netHandler = netHandler;
    }

    @Override
    protected Packet<?> toNormalPacket(NetByteBuf data) {
        return new CustomPayloadS2CPacket(PACKET_ID, data);
    }

    @Override
    protected Packet<?> toCompactPacket(int receiverId, NetByteBuf data) {
        byte[] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);
        return new CompactDataPacketToClient(receiverId, bytes);
    }

    @Override
    protected void sendPacket(Packet<?> packet) {
        netHandler.sendPacket(packet);
    }

    @Override
    public EnumNetSide getNetSide() {
        return EnumNetSide.SERVER;
    }

    @Override
    public String toString() {
        return "{ActiveServerConnection for " + netHandler.player + "}";
    }
}
