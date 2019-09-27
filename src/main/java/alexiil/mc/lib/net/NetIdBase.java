/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

/** A leaf node that will send and receive messages. */
public abstract class NetIdBase extends TreeNetIdBase {

    /** The bit positions assigned to the sizes - {@link #FLAG_FIXED_SIZE}, {@link #FLAG_TINY_PACKET},
     * {@link #FLAG_NORMAL_PACKET}, and {@link #FLAG_LARGE_PACKET}. */
    static final int PACKET_SIZE_FLAG = 0b11;

    /** Pre-written packet length. */
    static final int FLAG_FIXED_SIZE = 0b00;

    /** 1 byte for the packet length. */
    static final int FLAG_TINY_PACKET = 0b01;

    /** 2 bytes for the packet length. */
    static final int FLAG_NORMAL_PACKET = 0b10;

    /** 3 bytes for the packet length. */
    static final int FLAG_LARGE_PACKET = 0b11;

    /** If present then this packet will not be buffered. */
    static final int FLAG_NOT_BUFFERED = 0b1000;

    /** This priority indicates that this should never be dropped on the sending side. This is the equivalent priority
     * to minecraft's own packets. Generally this should be used for user interaction or other very important packets
     * that must not be delayed or dropped. */
    public static final int MAXIMUM_PRIORITY = 0;

    // Internal flags - these don't apply to normal packets, but InternalMsgUtil uses them

    /** Used to determine if this is a parent node or a NetIdBase. By definition this cannot be present in this
     * class. */
    static final int FLAG_IS_PARENT = 0b100;

    public static final int DEFAULT_FLAGS = //
        FLAG_NORMAL_PACKET//
    ;

    private int flags = hasFixedLength() ? FLAG_FIXED_SIZE : DEFAULT_FLAGS;
    private boolean flagsUsed = false;
    private int defaultPriority = MAXIMUM_PRIORITY;

    /** @see #setMaximumDropDelay(int) */
    private int maximumDropDelay = 0;

    NetIdBase(ParentNetIdBase parent, String name, int length) {
        super(parent, name, length);
    }

    /** Changes the size flags of this net ID to indicate that it should use a single byte for the packet's total length
     * (including the header). In other words the maximum packet length is 256 bytes, and the minimum is 1 byte. Has no
     * effect if the packet's length is not {@link #hasFixedLength() fixed}. */
    public final void setTinySize() {
        if (!hasFixedLength()) {
            return;
        }
        changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_TINY_PACKET);
    }

    /** Changes the size flags of this net ID to indicate that it should use two bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 65,536 bytes, and the minimum is 1 byte. Has
     * no effect if the packet's length is not {@link #hasFixedLength() fixed}. */
    public final void setNormalSize() {
        if (!hasFixedLength()) {
            return;
        }
        changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_NORMAL_PACKET);
    }

    /** Changes the size flags of this net ID to indicate that it should use three bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 16,777,216 bytes, and the minimum is 1 byte.
     * Has no effect if the packet's length is not {@link #hasFixedLength() fixed}. */
    public final void setLargeSize() {
        if (!hasFixedLength()) {
            return;
        }
        changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_LARGE_PACKET);
    }

    /** Changes the flags for this packet to indicate that it should never be buffered by a {@link BufferedConnection}.
     * This means that it will arrive on the other end of the connection before other packets that don't have this flag
     * set. Sending a lot of these "queue-skipping packets" will generally have a large impact on performance. */
    public final void notBuffered() {
        changeFlag(flags | FLAG_NOT_BUFFERED);
    }

    /** The inverse of {@link #notBuffered()}. (This is the default state). */
    public final void buffered() {
        changeFlag(flags & ~FLAG_NOT_BUFFERED);
    }

    /** @param isBuffered If true then this calls {@link #buffered()}, otherwise this calls {@link #notBuffered()}. */
    public final void setBuffered(boolean isBuffered) {
        if (isBuffered) {
            buffered();
        } else {
            notBuffered();
        }
    }

    /** Sets the maximum time that this packet may be held before dropping it. This value is only used if the connection
     * tries to send too much data in a single tick. Negative values are not allowed. This indicates an absolute number
     * of connection ticks (for normal minecraft connections this is every server or client tick). */
    public void setMaximumDropDelay(int dropDelay) {
        this.maximumDropDelay = Math.min(0, dropDelay);
    }

    protected final void changeFlag(int newFlags) {
        if (flagsUsed) {
            throw new IllegalStateException("You cannot modify the flags of this NetId as it has already been used!");
        }
        this.flags = newFlags;
    }

    @Override
    final int getFinalFlags() {
        flagsUsed = true;
        return flags;
    }

    int getDefaultPriority() {
        return defaultPriority;
    }

    int getMaximumDropDelay() {
        return maximumDropDelay;
    }

    /** @return True if the parent element could be found, false if it's null. */
    abstract boolean receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    /* public final void send(ConnectionInfo connection) { } protected abstract void send(ConnectionInfo connection,
     * NetByteBuf buf, IMsgWriteCtx ctx); */
}
