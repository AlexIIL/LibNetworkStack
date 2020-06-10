/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.ArrayDeque;
import java.util.Queue;

import javax.annotation.Nullable;

public abstract class BufferedConnection extends ActiveConnection {

    /** The minimum accepted value for {@link #ourMaxBandwidth} and {@link #theirMaxBandwidth}, in bytes per second. */
    private static final int MIN_BANDWIDTH = 8000;

    // for testing purposes
    public static final boolean ENABLE_QUEUE = true;

    final int defaultDropDelay;
    private int ourMaxBandwidth = MIN_BANDWIDTH;
    private int theirMaxBandwidth = MIN_BANDWIDTH;
    private int actualMaxBandwidth = MIN_BANDWIDTH;

    private final Queue<BufferedPacketInfo> packetQueue = new ArrayDeque<>();
    private int queueLength = 0;

    // private long lastTickTime;

    public BufferedConnection(ParentNetId rootId, int defaultDropDelay) {
        super(rootId);
        this.defaultDropDelay = defaultDropDelay;
    }

    public void setMaxBandwidth(int to) {
        if (to < MIN_BANDWIDTH) {
            to = MIN_BANDWIDTH;
        }
        ourMaxBandwidth = to;
        NetByteBuf data = NetByteBuf.buffer(6);
        data.writeVarUnsignedInt(InternalMsgUtil.ID_INTERNAL_NEW_BANDWIDTH);
        data.writeShort(to / MIN_BANDWIDTH);
        sendRawData0(data);
        data.release();
    }

    @Override
    protected final void sendPacket(NetByteBuf data, int packetId, @Nullable NetIdBase netId, int priority) {
        if (!ENABLE_QUEUE) {
            sendRawData0(data);
            return;
        }
        int rb = data.readableBytes();
        if (queueLength + rb > maximumPacketSize()) {
            flushQueue();
        }
        if (rb > maximumPacketSize()) {
            // Sending a huge packet
            // Instead of splitting it ourselves we'll just make the implementation do it
            sendRawData0(data);
        } else {
            packetQueue.add(new BufferedPacketInfo(data, priority));
            queueLength += rb;
            data.retain();

            if (netId != null && (netId.getFinalFlags() & NetIdBase.FLAG_NOT_BUFFERED) != 0) {
                flushQueue();
            }
        }
    }

    /** @return The maximum packet size that a single output packet can be. Used to try to keep the data sent to
     *         {@link #sendRawData0(NetByteBuf)} below this value when combining data into packets. */
    protected int maximumPacketSize() {
        // Default to a 65K, so that we don't end up with huge output packets.
        return (1 << 16) - 10;
    }

    /** Ticks this connection, flushing all queued data that needs to be sent immediately. */
    public void tick() {
        // long thisTick = System.currentTimeMillis();

        // FOR NOW
        // just empty the queue
        // rather than doing anything more complicated with bandwidth or priorities.
        // at the very least this should be an optimisation over just sending each individual
        // packet out one by one.
        sendTickPacket();
        flushQueue();
    }

    /** Optional method for subclasses to send additional packet before the queue is flushed. */
    protected void sendTickPacket() {}

    private void flushQueue() {
        if (!hasPackets()) {
            return;
        }
        if (packetQueue.size() == 1) {
            NetByteBuf data = packetQueue.remove().data;
            sendRawData0(data);
            data.release();
        } else {
            NetByteBuf combined = NetByteBuf.buffer(queueLength);
            BufferedPacketInfo bpi;
            while ((bpi = packetQueue.poll()) != null) {
                combined.writeBytes(bpi.data);
                bpi.data.release();
            }
            sendRawData0(combined);
            combined.release();
        }
        queueLength = 0;
    }

    protected final boolean hasPackets() {
        return !packetQueue.isEmpty();
    }

    @Override
    public void onReceiveRawData(NetByteBuf data) throws InvalidInputDataException {
        while (data.readableBytes() > 0) {
            InternalMsgUtil.onReceive(this, data);
        }
    }

    /** Sends some raw data. It might contain multiple packets, half packets, or even less. Either way the
     * implementation should just directly send the data on to the other side, and ensure it arrives in-order. */
    protected abstract void sendRawData0(NetByteBuf data);

    void updateTheirMaxBandwidth(int theirs) {
        theirs *= MIN_BANDWIDTH;
        theirMaxBandwidth = Math.max(theirs, MIN_BANDWIDTH);
        actualMaxBandwidth = Math.min(theirMaxBandwidth, ourMaxBandwidth);
    }

    static class BufferedPacketInfo {
        final NetByteBuf data;
        final int priority;
        // final boolean delayable;

        public BufferedPacketInfo(NetByteBuf data, int priority) {
            this.data = data;
            this.priority = priority;
        }
    }
}
