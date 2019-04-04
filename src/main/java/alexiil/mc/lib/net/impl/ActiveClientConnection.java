package alexiil.mc.lib.net.impl;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.util.PacketByteBuf;

public class ActiveClientConnection extends ActiveMinecraftConnection {
    public final ClientPlayNetworkHandler netHandler;

    public ActiveClientConnection(ClientPlayNetworkHandler netHandler) {
        // PacketContext is implemented through the mixin MixinServerPlayNetworkHandler
        super((PacketContext) netHandler);
        this.netHandler = netHandler;
    }

    @Override
    protected Packet<?> toNormalPacket(PacketByteBuf data) {
        return new CustomPayloadC2SPacket(PACKET_ID, data);
    }

    @Override
    protected Packet<?> toCompactPacket(int receiverId, ByteBuf data) {
        byte[] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);
        return new CompactDataPacketToServer(receiverId, bytes);
    }

    @Override
    protected void sendPacket(Packet<?> packet) {
        netHandler.sendPacket(packet);
    }

    @Override
    public NetworkSide getNetSide() {
        return NetworkSide.CLIENT;
    }
}
