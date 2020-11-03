/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public final class NetIdSignal extends NetIdSeparate {

    @FunctionalInterface
    public interface IMsgSignalReceiver {
        void handle(IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    private IMsgSignalReceiver receiver;

    public NetIdSignal(ParentNetId parent, String name) {
        super(parent, name, 0);
    }

    public NetIdSignal setReceiver(IMsgSignalReceiver receiver) {
        this.receiver = receiver;
        return this;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        validateSendingSide(connection);
        if (LibNetworkStack.CONFIG_RECORD_STACKTRACES && connection.sendStacktraces) {
            InternalMsgUtil.sendNextStacktrace(connection, new Throwable().fillInStackTrace());
        }
        InternalMsgUtil.send(connection, this, path, NetByteBuf.EMPTY_BUFFER);
    }

    // Internals

    @Override
    boolean receive(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(ctx);
        }
        return true;
    }

    @Override
    public NetIdSignal withoutBuffering() {
        notBuffered();
        return this;
    }

    @Override
    public NetIdSignal withTinySize() {
        setTinySize();
        return this;
    }

    @Override
    public NetIdSignal withNormalSize() {
        setNormalSize();
        return this;
    }

    @Override
    public NetIdSignal withLargeSize() {
        setLargeSize();
        return this;
    }

    @Override
    public NetIdSignal toClientOnly() {
        _toClientOnly();
        return this;
    }

    @Override
    public NetIdSignal toServerOnly() {
        _toServerOnly();
        return this;
    }

    @Override
    public NetIdSignal toEitherSide() {
        _toEitherSide();
        return this;
    }
}
