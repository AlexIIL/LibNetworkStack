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

public final class NetIdSignalK<T> extends NetIdTyped<T> {

    @FunctionalInterface
    public interface IMsgSignalReceiverK<T> {
        void handle(T obj, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    private IMsgSignalReceiverK<T> receiver;

    public NetIdSignalK(ParentNetIdSingle<T> parent, String name) {
        super(parent, name, 0);
    }

    public NetIdSignalK<T> setReceiver(IMsgSignalReceiverK<T> receiver) {
        this.receiver = receiver;
        return this;
    }

    @Override
    protected void receive(NetByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(obj, ctx);
        }
    }

    @Override
    public NetIdSignalK<T> withoutBuffering() {
        notBuffered();
        return this;
    }

    @Override
    public NetIdSignalK<T> withTinySize() {
        setTinySize();
        return this;
    }

    @Override
    public NetIdSignalK<T> withNormalSize() {
        setNormalSize();
        return this;
    }

    @Override
    public NetIdSignalK<T> withLargeSize() {
        setLargeSize();
        return this;
    }

    @Override
    public NetIdSignalK<T> toClientOnly() {
        _toClientOnly();
        return this;
    }

    @Override
    public NetIdSignalK<T> toServerOnly() {
        _toServerOnly();
        return this;
    }

    @Override
    public NetIdSignalK<T> toEitherSide() {
        _toEitherSide();
        return this;
    }

    /** Sends this signal over the specified connection */
    @Override
    public void send(ActiveConnection connection, T obj) {
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        validateSendingSide(ctx);
        NetByteBuf buffer = hasFixedLength() ? NetByteBuf.buffer(totalLength) : NetByteBuf.buffer();
        NetByteBuf bufferTypes = connection.sendTypes ? NetByteBuf.buffer() : null;
        CheckingNetByteBuf checkingBuffer = new CheckingNetByteBuf(buffer, bufferTypes);
        NetIdPath resolvedPath;
        if (parent.pathContainsDynamicParent) {
            List<TreeNetIdBase> nPath = new ArrayList<>();
            parent.writeDynamicContext(checkingBuffer, ctx, obj, nPath);
            nPath.add(this);
            resolvedPath = new NetIdPath(nPath);
        } else {
            parent.writeContextCall(checkingBuffer, ctx, obj);
            resolvedPath = path;
        }
        if (checkingBuffer.hasTypeData()) {
            int thisId = InternalMsgUtil.getWriteId(connection, this, resolvedPath);
            checkingBuffer.writeMarkerId(thisId);
        }
        if (bufferTypes != null) {
            InternalMsgUtil.sendNextTypes(connection, bufferTypes, checkingBuffer.getCountWrite());
        }
        if (LibNetworkStack.CONFIG_RECORD_STACKTRACES && connection.sendStacktraces) {
            InternalMsgUtil.createAndSendDebugThrowable(connection, ctx);
        }
        InternalMsgUtil.send(connection, this, resolvedPath, buffer);
        buffer.release();
    }
}
