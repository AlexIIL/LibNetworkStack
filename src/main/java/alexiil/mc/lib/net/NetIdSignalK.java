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
        NetByteBuf buffer = hasFixedLength() ? NetByteBuf.buffer(totalLength) : NetByteBuf.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        validateSendingSide(ctx);
        if (parent.pathContainsDynamicParent) {
            List<TreeNetIdBase> nPath = new ArrayList<>();
            parent.writeDynamicContext(buffer, ctx, obj, nPath);
            nPath.add(this);
            TreeNetIdBase[] array = nPath.toArray(new TreeNetIdBase[0]);
            InternalMsgUtil.send(connection, this, new NetIdPath(array), buffer);
        } else {
            parent.writeContext(buffer, ctx, obj);
            InternalMsgUtil.send(connection, this, path, buffer);
        }
        buffer.release();
    }
}
