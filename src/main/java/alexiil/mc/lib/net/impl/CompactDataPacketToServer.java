/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import alexiil.mc.lib.net.NetByteBuf;

public class CompactDataPacketToServer implements IPacketCustomId<ServerPlayNetworkHandler> {

    private final int clientExpectedId;
    private byte[] payload;

    public CompactDataPacketToServer(int clientExpectedId, byte[] payload) {
        this.clientExpectedId = clientExpectedId;
        this.payload = payload;
    }

    /** Used for reading */
    public CompactDataPacketToServer(PacketByteBuf buf) {
        clientExpectedId = 0;
        payload = new byte[buf.readableBytes()];
        buf.readBytes(payload);
    }

    @Override
    public int getReadId() {
        return clientExpectedId;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBytes(payload);
    }

    @Override
    public void apply(ServerPlayNetworkHandler netHandler) {
        NetByteBuf buffer = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload));
        CoreMinecraftNetUtil.onServerReceivePacket(netHandler, buffer);
        buffer.release();
    }
}
