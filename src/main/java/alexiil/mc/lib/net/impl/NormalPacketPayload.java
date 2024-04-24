package alexiil.mc.lib.net.impl;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record NormalPacketPayload(byte[] payload) implements CustomPayload {
    public static final PacketCodec<PacketByteBuf, NormalPacketPayload> CODEC =
        PacketCodec.of(NormalPacketPayload::write, NormalPacketPayload::read);
    public static final Id<NormalPacketPayload> ID = new Id<>(ActiveMinecraftConnection.PACKET_ID);

    public static NormalPacketPayload read(PacketByteBuf buf) {
        byte[] payload = new byte[buf.readVarInt()];
        buf.readBytes(payload);
        return new NormalPacketPayload(payload);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        // we can't be sure this payload won't be bundled with others
        buf.writeVarInt(payload.length);
        buf.writeBytes(payload);
    }
}
