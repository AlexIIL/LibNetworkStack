/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Util;

import alexiil.mc.lib.net.EnumNetSide;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.mixin.impl.MinecraftClientMixin;

public class ActiveClientConnection extends ActiveMinecraftConnection {

    private static final int DATAPOINT_SATURATION_COUNT = 30;

    public final ClientPlayNetworkHandler netHandler;

    private long currentServerTick = Long.MIN_VALUE, prevServerTick;
    private long currentServerSendTime, prevServerSendTime;
    private long currentServerReceiveTime, prevServerReceiveTime;

    private double averageSendTimeDelta;
    private double averageReceiveTimeDelta;

    private int datapointCount;

    private long lastClientMs = Long.MIN_VALUE;

    private long smoothedServerTickValue = Long.MIN_VALUE;
    private double smoothedServerTickDelta = 0;

    public ActiveClientConnection(ClientPlayNetworkHandler netHandler) {
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
    public PlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    @Override
    public String toString() {
        // There's nothing really all that useful to show here - there will normally be only one.
        String hex = Integer.toHexString(System.identityHashCode(this));
        String prefix = (MinecraftClient.getInstance().getNetworkHandler() == netHandler) ? "The" : "Other";
        return "{" + prefix + " ActiveClientConnection@" + hex + "}";
    }

    void receiveServerTick(long tick, long sendTime) {
        if (lastClientMs == Long.MIN_VALUE) {
            lastClientMs = Util.getMeasuringTimeMs();
        }

        prevServerTick = currentServerTick;
        prevServerSendTime = currentServerSendTime;
        prevServerReceiveTime = currentServerReceiveTime;

        currentServerTick = tick;
        currentServerSendTime = sendTime;
        currentServerReceiveTime = lastClientMs;

        if (datapointCount < 1) {
            datapointCount = 1;

            smoothedServerTickValue = tick;
            smoothedServerTickDelta = 0;

            return;
        } else if (datapointCount == 1) {
            datapointCount++;

            averageSendTimeDelta = currentServerSendTime - prevServerSendTime;
            averageReceiveTimeDelta = currentServerReceiveTime - prevServerReceiveTime;

            return;
        } else if (datapointCount < DATAPOINT_SATURATION_COUNT) {
            datapointCount++;
        }

        double divisor = DATAPOINT_SATURATION_COUNT;

        averageSendTimeDelta
            = (averageSendTimeDelta * datapointCount + currentServerSendTime - prevServerSendTime) / divisor;

        averageReceiveTimeDelta
            = (averageReceiveTimeDelta * datapointCount + currentServerReceiveTime - prevServerReceiveTime) / divisor;
    }

    /** Called by {@link MinecraftClientMixin} (<strong>ONLY</strong>) when minecraft increases it's
     * {@link MinecraftClient#getTickDelta()} value. Used internally to update the values returned by
     * {@link #getSmoothedServerTickValue()} and {@link #getSmoothedServerTickDelta()}.
     * 
     * @param milliseconds The value of {@link Util#getMeasuringTimeMs()} that was passed into
     *            {@link RenderTickCounter#beginRenderTick(long)}. */
    public void onIncrementMinecraftTickCounter(long milliseconds) {
        if (datapointCount <= 0) {
            lastClientMs = milliseconds;
            return;
        }

        if (datapointCount == 1) {
            // The average's won't have populated so there's not much we can do
            lastClientMs = milliseconds;
            return;
        }

        // Now we have an average of the tick gap and network gap
        // For now just ignore the receive time deltas

        // First try: Very basic, used to test everything

        smoothedServerTickValue = currentServerTick;
        smoothedServerTickDelta = 0;
        return;
    }

    /** Gets the last received tick that the server has sent. It is never normally a good idea to use this method
     * directly because this will not return useful values if the network connection isn't perfect. Instead use
     * {@link #getSmoothedServerTickValue()} and {@link #getSmoothedServerTickDelta()}.
     * 
     * @return The last received tick from the server, or {@link Long#MIN_VALUE} if the server hasn't sent tick data
     *         yet. */
    public long getAbsoluteServerTick() {
        return currentServerTick;
    }

    /** @return The smoothed tick value for the server. This will always lag behind {@link #getAbsoluteServerTick()},
     *         but a best-effort is made to try and keep the rate that this changes roughly equal to the server tick
     *         speed, rather than being solely based on the network connection. */
    public long getSmoothedServerTickValue() {
        return smoothedServerTickValue;
    }

    /** @return A value between 0 and 1 (to be used along side {@link #getSmoothedServerTickValue()}) which is analogous
     *         to {@link MinecraftClient#getTickDelta()}. */
    public double getSmoothedServerTickDelta() {
        return smoothedServerTickDelta;
    }
}
