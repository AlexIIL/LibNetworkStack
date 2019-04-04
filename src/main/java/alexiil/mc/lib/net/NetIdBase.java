package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

/** A leaf node that will send and receive messages. */
public abstract class NetIdBase extends TreeNetIdBase {

    public static final int PACKET_SIZE_FLAG = 0b11;

    /** If this flag is set then the packets' length is not written out - instead it is taken from
     * {@link TreeNetIdBase#totalLength}, which cannot exceed {@link #MAXIMUM_PACKET_LENGTH}. */
    public static final int FLAG_FIXED_SIZE = 0b00;

    /** If this flag is set then the packet's length is written out in 1 byte. In other words the maximum packet length
     * - including it's header - is 256 bytes. The minimum packet length is 1 byte. Has no effect if the packet's length
     * is {@link #DYNAMIC_LENGTH}. */
    public static final int FLAG_TINY_PACKET = 0b01;

    /** If this flag is set then the packet's length is written out in 2 bytes. In other words the maximum packet length
     * - including it's header - is 65,536 bytes, or 65KB. The minimum packet length is 1 byte Has no effect if the
     * packet's length is {@link #DYNAMIC_LENGTH}. */
    public static final int FLAG_NORMAL_PACKET = 0b10;

    /** This this flag is set then this the packet length is encoded in 3 bytes. In other words the maximum packet
     * length - including it's header - is 16,777,216 bytes, or 16 MB (or {@link #MAXIMUM_PACKET_LENGTH}). The minimum
     * packet length is 1 byte. Has no effect if the packet's length is {@link #DYNAMIC_LENGTH}. */
    public static final int FLAG_LARGE_PACKET = 0b11;

    /** This flag indicates that this should skip any and all buffer queue's and be immediately This means that it will
     * arrive on the other end of the connection before other packets that don't have this flag set. */
    public static final int FLAG_SKIP_QUEUE_PACKET = 0b1000;

    /** This priority indicates that this should never be dropped on the sending side. This is the equivalent priority
     * to minecraft's own packets. Generally this should be used for user interaction or other very important packets
     * that must not be delayed or dropped. */
    public static final int MAXIMUM_PRIORITY = 0;

    /** A marker for the default drop delay between sending this packet and deciding that it should be dropped. The
     * actual value for this number is determined by the {@link ActiveConnection}.
     * <p>
     * Negative numbers indicate a multiplier for the active connection's default drop delay, divided by 100. So a value
     * of -100 would use the default drop delay of the connection. */
    public static final int DEFAULT_DROP_DELAY = -100;

    // Internal flags - these don't apply to normal packets, but InternalMsgUtil uses them

    /** Used to determine if this is a parent node or a NetIdBase. By definition this cannot be present in this
     * class. */
    static final int FLAG_IS_PARENT = 0b100;

    public static final int DEFAULT_FLAGS = //
        FLAG_NORMAL_PACKET//
    ;

    private int flags = hasFixedLength() ? FLAG_FIXED_SIZE : DEFAULT_FLAGS;
    private int defaultPriority = MAXIMUM_PRIORITY;

    /** The default minimum drop delay. Only has an effect if the {@link ActiveConnection} is a
     * {@link BufferedConnection}. */
    private int minimumDropDelay = DEFAULT_DROP_DELAY;

    NetIdBase(ParentNetIdBase parent, String name, int length) {
        super(parent, name, length);
    }

    @Override
    int getFlags() {
        return flags;
    }

    int getDefaultPriority() {
        return defaultPriority;
    }

    int getMinimumDropDelay() {
        return minimumDropDelay;
    }

    /** @return True if the parent element could be found, false if it's null. */
    public abstract boolean receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    /* public final void send(ConnectionInfo connection) { } protected abstract void send(ConnectionInfo connection,
     * ByteBuf buf, IMsgWriteCtx ctx); */
}
