package alexiil.mc.lib.net.impl;

import java.io.IOException;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.PacketByteBuf;

import alexiil.mc.lib.net.NetByteBuf;

public class CompactDataPacketToServer implements IPacketCustomId<ServerPlayNetworkHandler> {

    private final int clientExpectedId;
    private byte[] payload;

    /** Used for reading */
    public CompactDataPacketToServer() {
        clientExpectedId = 0;
    }

    public CompactDataPacketToServer(int clientExpectedId, byte[] payload) {
        this.clientExpectedId = clientExpectedId;
        this.payload = payload;
    }

    @Override
    public int getReadId() {
        return clientExpectedId;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        payload = new byte[buf.readableBytes()];
        buf.readBytes(payload);
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        buf.writeBytes(payload);
    }

    @Override
    public void apply(ServerPlayNetworkHandler netHandler) {
        NetByteBuf buffer = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload));
        CoreMinecraftNetUtil.onServerReceivePacket((PacketContext) netHandler, buffer);
        buffer.release();
    }
}
