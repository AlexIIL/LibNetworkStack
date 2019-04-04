package alexiil.mc.lib.net;

import java.util.ArrayDeque;
import java.util.Queue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
        ByteBuf data = Unpooled.buffer(6);
        data.writeInt(InternalMsgUtil.ID_INTERNAL_NEW_BANDWIDTH);
        data.writeShort(to / MIN_BANDWIDTH);
        sendRawData0(data);
        data.release();
    }

    @Override
    public final void sendPacket(ByteBuf data, int packetId, NetIdBase netId, int priority) {
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
        }
    }

    /** @return The maximum packet size that a single output packet can be. Used to try to keep the data sent to
     *         {@link #sendRawData0(ByteBuf)} below this value when combining data into packets. */
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
        flushQueue();
    }

    private void flushQueue() {
        if (packetQueue.isEmpty()) {
            return;
        }
        if (packetQueue.size() == 1) {
            ByteBuf data = packetQueue.remove().data;
            sendRawData0(data);
            data.release();
        } else {
            ByteBuf combined = Unpooled.buffer(queueLength);
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

    @Override
    public void onReceiveRawData(ByteBuf data) throws InvalidInputDataException {
        while (data.readableBytes() > 0) {
            InternalMsgUtil.onReceive(this, data);
        }
    }

    /** Sends some raw data. It might contain multiple packets, half packets, or even less. Either way the
     * implementation should just directly send the data on to the other side, and ensure it arrives in-order. */
    protected abstract void sendRawData0(ByteBuf data);

    void updateTheirMaxBandwidth(int theirs) {
        theirs *= MIN_BANDWIDTH;
        theirMaxBandwidth = Math.max(theirs, MIN_BANDWIDTH);
        actualMaxBandwidth = Math.min(theirMaxBandwidth, ourMaxBandwidth);
    }

    static class BufferedPacketInfo {
        final ByteBuf data;
        final int priority;
        // final boolean delayable;

        public BufferedPacketInfo(ByteBuf data, int priority) {
            this.data = data;
            this.priority = priority;
        }
    }
}
