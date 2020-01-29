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
        NetByteBuf buffer = hasFixedLength() ? NetByteBuf.buffer(totalLength) : NetByteBuf.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        validateSendingSide(ctx);
        final NetIdPath resolvedPath;
        if (parent.pathContainsDynamicParent) {
            List<TreeNetIdBase> nPath = new ArrayList<>();
            parent.writeDynamicContext(buffer, ctx, obj, nPath);
            nPath.add(this);
            TreeNetIdBase[] array = nPath.toArray(new TreeNetIdBase[0]);
            resolvedPath = new NetIdPath(array);
        } else {
            parent.writeContext(buffer, ctx, obj);
            resolvedPath = this.path;
        }
        int headerLength = buffer.writerIndex();
        writer.write(obj, buffer, ctx);
        if (headerLength != buffer.writerIndex()) {
            // Only send data packets if anything was actually written.
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
