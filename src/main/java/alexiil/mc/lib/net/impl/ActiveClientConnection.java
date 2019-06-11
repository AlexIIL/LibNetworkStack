package alexiil.mc.lib.net.impl;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;

import alexiil.mc.lib.net.EnumNetSide;
import alexiil.mc.lib.net.NetByteBuf;

public class ActiveClientConnection extends ActiveMinecraftConnection {
    public final ClientPlayNetworkHandler netHandler;

    public ActiveClientConnection(ClientPlayNetworkHandler netHandler) {
        // PacketContext is implemented through the mixin MixinServerPlayNetworkHandler
        super((PacketContext) netHandler);
        this.netHandler = netHandler;
    }

    @Override
    protected Packet<?> toNormalPacket(NetByteBuf data) {
        return new CustomPayloadC2SPacket(PACKET_ID, data);
    }

    @Override
    protected Packet<?> toCompactPacket(int receiverId, NetByteBuf data) {
        byte[] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);
        return new CompactDataPacketToServer(receiverId, bytes);
    }

    @Override
    protected void sendPacket(Packet<?> packet) {
        netHandler.sendPacket(packet);
    }

    @Override
    public EnumNetSide getNetSide() {
        return EnumNetSide.CLIENT;
    }

    @Override
    public String toString() {
        // There's nothing really all that useful to show here - there will normally be only one.
        String hex = Integer.toHexString(System.identityHashCode(this));
        String prefix = (MinecraftClient.getInstance().getNetworkHandler() == netHandler) ? "The" : "Other";
        return "{" + prefix + " ActiveClientConnection@" + hex + "}";
    }
}
