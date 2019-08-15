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

    @Override
    boolean receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(ctx);
        }
        return true;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        InternalMsgUtil.send(connection, this, path, NetByteBuf.EMPTY_BUFFER);
    }
}
