/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.List;

public final class NetIdDataK<T> extends NetIdTyped<T> {

    @FunctionalInterface
    public interface IMsgDataReceiverK<T> {
        void receive(T obj, NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriterK<T> {
        void write(T obj, NetByteBuf buffer, IMsgWriteCtx ctx);
    }

    private IMsgDataReceiverK<T> receiver = (t, buffer, ctx) -> {
        throw new InvalidInputDataException("No receiver set for " + ctx.getNetSide());
    };
    private IMsgDataWriterK<T> writer;

    public NetIdDataK(ParentNetIdSingle<T> parent, String name, int length) {
        super(parent, name, length);
    }

    public NetIdDataK<T> setReceiver(IMsgDataReceiverK<T> receiver) {
        this.receiver = receiver;
        return this;
    }

    public NetIdDataK<T> setReadWrite(IMsgDataReceiverK<T> receiver, IMsgDataWriterK<T> writer) {
        this.receiver = receiver;
        this.writer = writer;
        return this;
    }

    @Override
    public void receive(NetByteBuf buffer, IMsgReadCtx ctx, T parentValue) throws InvalidInputDataException {
        receiver.receive(parentValue, buffer, ctx);
    }

    /** Sends this signal over the specified connection */
    @Override
    public void send(ActiveConnection connection, T obj) {
        send(connection, obj, writer);
    }

    public void send(ActiveConnection connection, T obj, IMsgDataWriterK<T> writer) {
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        validateSendingSide(ctx);
        NetByteBuf buffer = hasFixedLength() ? connection.allocBuffer(totalLength) : connection.allocBuffer();
        NetByteBuf bufferTypes = connection.sendTypes ? NetByteBuf.buffer() : null;
        CheckingNetByteBuf checkingBuffer = new CheckingNetByteBuf(buffer, bufferTypes);
        final NetIdPath resolvedPath;
        if (parent.pathContainsDynamicParent) {
            List<TreeNetIdBase> nPath = new ArrayList<>();
            parent.writeDynamicContext(checkingBuffer, ctx, obj, nPath);
            nPath.add(this);
            resolvedPath = new NetIdPath(nPath);
        } else {
            parent.writeContextCall(checkingBuffer, ctx, obj);
            resolvedPath = this.path;
        }
        if (checkingBuffer.hasTypeData()) {
            int thisId = InternalMsgUtil.getWriteId(connection, this, resolvedPath);
            checkingBuffer.writeMarkerId(thisId);
        }
        int headerLength = checkingBuffer.writerIndex();
        int headerBitLength = checkingBuffer.getBitWriterIndex();
        writer.write(obj, checkingBuffer, ctx);
        if (headerLength != checkingBuffer.writerIndex() || headerBitLength != checkingBuffer.getBitWriterIndex()) {
            // Only send data packets if anything was actually written.
            if (bufferTypes != null) {
                InternalMsgUtil.sendNextTypes(connection, bufferTypes, checkingBuffer.getCountWrite());
            }
            // begin TMP_FIX_BLANKETCON_2022_ALEX_01
            // TO REVERT:
            // 1: Uncomment the removed part
            if (/*LibNetworkStack.CONFIG_RECORD_STACKTRACES && */connection.sendStacktraces) {
                // 2: Remove the next line
                InternalMsgUtil.createAndSendDebugThrowable(connection, ctx);
                // 3: Uncomment the next line
                // InternalMsgUtil.sendNextStacktrace(connection, new Throwable().fillInStackTrace());
                // end TMP_FIX_BLANKETCON_2022_ALEX_01
            }
            InternalMsgUtil.send(connection, this, resolvedPath, buffer);
        }
        buffer.release();
    }

    @Override
    public NetIdDataK<T> withoutBuffering() {
        notBuffered();
        return this;
    }

    @Override
    public NetIdDataK<T> withTinySize() {
        setTinySize();
        return this;
    }

    @Override
    public NetIdDataK<T> withNormalSize() {
        setNormalSize();
        return this;
    }

    @Override
    public NetIdDataK<T> withLargeSize() {
        setLargeSize();
        return this;
    }

    @Override
    public NetIdDataK<T> toClientOnly() {
        _toClientOnly();
        return this;
    }

    @Override
    public NetIdDataK<T> toServerOnly() {
        _toServerOnly();
        return this;
    }

    @Override
    public NetIdDataK<T> toEitherSide() {
        _toEitherSide();
        return this;
    }
}
