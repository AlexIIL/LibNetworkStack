/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import java.io.IOException;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import alexiil.mc.lib.net.NetByteBuf;

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
        NetByteBuf buffer = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload));
        CoreMinecraftNetUtil.onClientReceivePacket__old((PacketContext) netHandler, buffer);
        buffer.release();
    }
}
