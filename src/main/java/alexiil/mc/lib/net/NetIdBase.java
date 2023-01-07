/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

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

    /** The bit positions assigned to the sides - {@link #FLAG_SIDED_RECV_ON_CLIENT} and
     * {@link #FLAG_SIDED_RECV_ON_SERVER}. */
    static final int FLAGS_SIDED = 0b11_0000;

    /** If present then this packet may only be sent from the server to the client. */
    static final int FLAG_SIDED_RECV_ON_CLIENT = 0b01_0000;

    /** If present then this packet may only be sent from the client to the server. */
    static final int FLAG_SIDED_RECV_ON_SERVER = 0b10_0000;

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
     * effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * Generally you should call {@link #withTinySize()} rather than this, as it returns itself rather than nothing.
     * 
     * @see #setNormalSize()
     * @see #setLargeSize() */
    public final void setTinySize() {
        if (!hasFixedLength()) {
            changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_TINY_PACKET);
        }
    }

    /** Changes the size flags of this net ID to indicate that it should use a single byte for the packet's total length
     * (including the header). In other words the maximum packet length is 256 bytes, and the minimum is 1 byte. Has no
     * effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * This method should be preferred over {@link #setTinySize()} because it returns itself.
     * 
     * @see #withNormalSize()
     * @see #withLargeSize() */
    public abstract NetIdBase withTinySize();

    /** Changes the size flags of this net ID to indicate that it should use two bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 65,536 bytes, and the minimum is 1 byte. Has
     * no effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * Generally you should call {@link #withNormalSize()} rather than this, as it returns itself rather than nothing.
     * 
     * @see #setTinySize()
     * @see #setLargeSize() */
    public final void setNormalSize() {
        if (!hasFixedLength()) {
            changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_NORMAL_PACKET);
        }
    }

    /** Changes the size flags of this net ID to indicate that it should use two bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 65,536 bytes, and the minimum is 1 byte. Has
     * no effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * This method should be preferred over {@link #setNormalSize()} because it returns itself.
     * 
     * @see #withTinySize()
     * @see #withLargeSize() */
    public abstract NetIdBase withNormalSize();

    /** Changes the size flags of this net ID to indicate that it should use three bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 16,777,216 bytes, and the minimum is 1 byte.
     * Has no effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * Generally you should call {@link #withLargeSize()} rather than this, as it returns itself rather than nothing.
     * 
     * @see #setTinySize()
     * @see #setNormalSize() */
    public final void setLargeSize() {
        if (!hasFixedLength()) {
            changeFlag(flags & ~PACKET_SIZE_FLAG | FLAG_LARGE_PACKET);
        }
    }

    /** Changes the size flags of this net ID to indicate that it should use three bytes for the packet's total length
     * (including the header). In other words the maximum packet length is 16,777,216 bytes, and the minimum is 1 byte.
     * Has no effect if the packet's length is not {@link #hasFixedLength() fixed}.
     * <p>
     * Unlike all of the other flag modification methods this may be called at any time, in particular before and after
     * sending this.
     * <p>
     * This method should be preferred over {@link #setLargeSize()} because it returns itself.
     * 
     * @see #withTinySize()
     * @see #withNormalSize() */
    public abstract NetIdBase withLargeSize();

    /** Changes the flags for this packet to indicate that it should never be buffered by a {@link BufferedConnection}.
     * This means that it will case {@link ActiveConnection#flushQueue()} to be called after this is sent. Please note
     * that this <em>does</em> keep packet ordering - any previously written packets (which were buffered) will be read
     * before this one is read. Sending a lot of these "queue-flushing packets" will generally have a large impact on
     * performance.
     * <p>
     * Unlike all of the other flag modification methods this may be called at any time, in particular before and after
     * sending this.
     * <p>
     * Generally you should call {@link #withoutBuffering()} rather than this, as it returns itself rather than
     * nothing. */
    public final void notBuffered() {
        flags |= FLAG_NOT_BUFFERED;
    }

    /** The inverse of {@link #notBuffered()}. (This is the default state).
     * <p>
     * Unlike all of the other flag modification methods this may be called at any time, in particular before and after
     * sending this. */
    public final void buffered() {
        flags &= ~FLAG_NOT_BUFFERED;
    }

    /** @param isBuffered If true then this calls {@link #buffered()}, otherwise this calls {@link #notBuffered()}. */
    public final void setBuffered(boolean isBuffered) {
        if (isBuffered) {
            buffered();
        } else {
            notBuffered();
        }
    }

    /** Changes the flags for this packet to indicate that it should never be buffered by a {@link BufferedConnection}.
     * This means that it will case {@link ActiveConnection#flushQueue()} to be called after this is sent. Please note
     * that this <em>does</em> keep packet ordering - any previously written packets (which were buffered) will be read
     * before this one is read. Sending a lot of these "queue-flushing packets" will generally have a large impact on
     * performance.
     * <p>
     * Unlike all of the other flag modification methods this may be called at any time, in particular before and after
     * sending this.
     * <p>
     * This method should be preferred over {@link #notBuffered()} because it returns itself.
     * 
     * @return This. */
    public abstract NetIdBase withoutBuffering();

    /** Impl for {@link #toClientOnly()} */
    protected final void _toClientOnly() {
        changeFlag(flags & ~FLAGS_SIDED | FLAG_SIDED_RECV_ON_CLIENT);
    }

    /** Impl for {@link #toServerOnly()} */
    protected final void _toServerOnly() {
        changeFlag(flags & ~FLAGS_SIDED | FLAG_SIDED_RECV_ON_SERVER);
    }

    /** Impl for {@link #toEitherSide()} */
    protected final void _toEitherSide() {
        changeFlag(flags & ~FLAGS_SIDED);
    }

    /** Changes the flags for this packet to indicate that it should only be sent from the server to the client. This
     * will make sending this packet from the client throw an unchecked exception, and make the server refuse to bind
     * this packet to an integer ID for receiving.
     * 
     * @see #toServerOnly() */
    public abstract NetIdBase toClientOnly();

    /** Changes the flags for this packet to indicate that it should only be sent from the client to the server. This
     * will make sending this packet from the server throw an unchecked exception, and make the client refuse to bind
     * this packet to an integer ID for receiving.
     * 
     * @see #toClientOnly() */
    public abstract NetIdBase toServerOnly();

    /** This clears the {@link #toClientOnly()} and {@link #toServerOnly()} flag states, and will make sending packets
     * not throw exceptions.
     * <p>
     * This is the default state for every {@link NetIdBase}. */
    public abstract NetIdBase toEitherSide();

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

    final void validateSendingSide(IMsgWriteCtx ctx) {
        validateSendingSide(ctx.getConnection());
    }

    final void validateSendingSide(ActiveConnection connection) {
        int single = flags & FLAGS_SIDED;
        if (single == 0) {
            return;
        }
        if (!(connection instanceof ActiveMinecraftConnection)) {
            throw new IllegalStateException("You can only send " + this + " through a minecraft connection! (Was given " + connection + ")");
        }
        ActiveMinecraftConnection a = (ActiveMinecraftConnection) connection;
        if (single == FLAG_SIDED_RECV_ON_CLIENT) {
            if (a.getNetSide() == EnumNetSide.CLIENT) {
                throw new IllegalStateException("Cannot write " + this + " on the client!");
            }
        } else if (single == FLAG_SIDED_RECV_ON_SERVER) {
            if (a.getNetSide() == EnumNetSide.SERVER) {
                throw new IllegalStateException("Cannot write " + this + " on the server!");
            }
        }
    }

    /** @return True if the parent element could be found, false if it's null. */
    abstract boolean receive(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
}
