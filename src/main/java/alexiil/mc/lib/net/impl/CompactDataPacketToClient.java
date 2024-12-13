/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import io.netty.buffer.Unpooled;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import alexiil.mc.lib.net.NetByteBuf;

public class CompactDataPacketToClient implements IPacketCustomId<ClientPlayPacketListener> {
    
    public static final PacketCodec<PacketByteBuf, CompactDataPacketToClient> CODEC =
        PacketCodec.of(CompactDataPacketToClient::write, CompactDataPacketToClient::new);
    public static final PacketType<CompactDataPacketToClient> TYPE =
        new PacketType<>(NetworkSide.CLIENTBOUND, ActiveMinecraftConnection.PACKET_ID);

    private final int serverExpectedId;
    private byte[] payload;

    public CompactDataPacketToClient(int serverExpectedId, byte[] payload) {
        this.serverExpectedId = serverExpectedId;
        this.payload = payload;
    }

    /** Used for reading */
    public CompactDataPacketToClient(PacketByteBuf buf) {
        serverExpectedId = 0;
        payload = new byte[buf.readableBytes()];
        buf.readBytes(payload);
    }

    @Override
    public int getReadId() {
        return serverExpectedId;
    }

    @Override
    public PacketType<? extends Packet<ClientPlayPacketListener>> getPacketType() {
        return TYPE;
    }

    public void write(PacketByteBuf buf) {
        buf.writeBytes(payload);
    }

    @Override
    public void apply(ClientPlayPacketListener netHandler) {
        NetByteBuf buffer = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload));
        CoreMinecraftNetUtil.onClientReceivePacket(netHandler, buffer);
        buffer.release();
    }
}
