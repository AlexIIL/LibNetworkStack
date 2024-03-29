/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

import alexiil.mc.lib.net.EnumNetSide;
import alexiil.mc.lib.net.NetByteBuf;

/** A connection on the server side to a specific {@link ServerPlayerEntity}. */
public class ActiveServerConnection extends ActiveMinecraftConnection {
    public final ServerPlayNetworkHandler netHandler;

    private long serverTick = Long.MIN_VALUE;

    public ActiveServerConnection(ServerPlayNetworkHandler netHandler) {
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
    public PlayerEntity getPlayer() {
        return netHandler.player;
    }

    @Override
    public EnumNetSide getNetSide() {
        return EnumNetSide.SERVER;
    }

    @Override
    public String toString() {
        return "{ActiveServerConnection for " + netHandler.player + "}";
    }

    /** @return The value that the client will use to identify the current server tick. */
    public long getServerTick() {
        if (serverTick == Long.MIN_VALUE) {
            serverTick = 0;
        }
        return serverTick;
    }

    @Override
    protected void sendTickPacket() {
        super.sendTickPacket();
        getServerTick();
        if (serverTick != Long.MIN_VALUE) {
            final long sv = serverTick;
            final long now = Util.getMeasuringTimeMs();
            NET_ID_SERVER_TICK.send(this, (buffer, ctx) -> {
                buffer.writeLong(sv);
                buffer.writeLong(now);
            });
            serverTick++;
            if (serverTick < 0) {
                // Overflow, this probably won't end well as we use min_value as the sentinal value.
                serverTick++;
            }
        }
    }
}
