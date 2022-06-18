/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public final class NetIdData extends NetIdSeparate {

    @FunctionalInterface
    public interface IMsgDataReceiver {
        void receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriter {
        void write(NetByteBuf buffer, IMsgWriteCtx ctx);
    }

    private IMsgDataReceiver receiver = (buffer, ctx) -> {
        throw new InvalidInputDataException("No receiver set!");
    };
    private IMsgDataWriter writer;

    public NetIdData(ParentNetId parent, String name, int length) {
        super(parent, name, length);
    }

    public NetIdData setReceiver(IMsgDataReceiver receiver) {
        this.receiver = receiver;
        return this;
    }

    public NetIdData setReadWrite(IMsgDataReceiver receiver, IMsgDataWriter writer) {
        this.receiver = receiver;
        this.writer = writer;
        return this;
    }

    @Override
    boolean receive(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        receiver.receive(buffer, ctx);
        return true;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        send(connection, writer);
    }

    public void send(ActiveConnection connection, IMsgDataWriter writer) {
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        validateSendingSide(ctx);
        NetByteBuf buffer = hasFixedLength() ? connection.allocBuffer(totalLength) : connection.allocBuffer();
        CheckingNetByteBuf checkingBuffer = null;
        if (connection.sendTypes) {
            checkingBuffer = new CheckingNetByteBuf(buffer, NetByteBuf.buffer());
        }
        writer.write(checkingBuffer == null ? buffer : checkingBuffer, ctx);
        if (buffer.readableBytes() > 0) {
            // Only send data packets if anything was actually written.
            if (checkingBuffer != null) {
                InternalMsgUtil.sendNextTypes(connection, checkingBuffer.typeBuffer, checkingBuffer.getCountWrite());
            }
            if (LibNetworkStack.CONFIG_RECORD_STACKTRACES && connection.sendStacktraces) {
                InternalMsgUtil.sendNextStacktrace(connection, new Throwable().fillInStackTrace());
            }
            InternalMsgUtil.send(connection, this, path, buffer);
        }
        buffer.release();
    }

    @Override
    public NetIdData withoutBuffering() {
        notBuffered();
        return this;
    }

    @Override
    public NetIdData withTinySize() {
        setTinySize();
        return this;
    }

    @Override
    public NetIdData withNormalSize() {
        setNormalSize();
        return this;
    }

    @Override
    public NetIdData withLargeSize() {
        setLargeSize();
        return this;
    }

    @Override
    public NetIdData toClientOnly() {
        _toClientOnly();
        return this;
    }

    @Override
    public NetIdData toServerOnly() {
        _toServerOnly();
        return this;
    }

    @Override
    public NetIdData toEitherSide() {
        _toEitherSide();
        return this;
    }
}
