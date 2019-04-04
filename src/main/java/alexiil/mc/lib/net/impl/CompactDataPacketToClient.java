package alexiil.mc.lib.net.impl;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.PacketByteBuf;

public class CompactDataPacketToClient implements IPacketCustomId<ClientPlayNetworkHandler> {

    private final int serverExpectedId;
    private byte[] payload;

    /** Used for reading */
    public CompactDataPacketToClient() {
        serverExpectedId = 0;
    }

    public CompactDataPacketToClient(int serverExpectedId, byte[] payload) {
        this.serverExpectedId = serverExpectedId;
        this.payload = payload;
    }

    @Override
    public int getReadId() {
        return serverExpectedId;
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
    public void apply(ClientPlayNetworkHandler netHandler) {
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);
        CoreMinecraftNetUtil.onClientReceivePacket((PacketContext) netHandler, buffer);
        buffer.release();
    }
}
