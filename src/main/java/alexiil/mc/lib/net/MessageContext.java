/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public abstract class MessageContext implements IMsgCtx {

    public final ActiveConnection connection;
    public final NetIdBase id;

    public MessageContext(ActiveConnection connection, NetIdBase id) {
        this.connection = connection;
        this.id = id;
    }

    @Override
    public ActiveConnection getConnection() {
        return connection;
    }

    @Override
    public NetIdBase getNetId() {
        return id;
    }

    public static class Read extends MessageContext implements IMsgReadCtx {
        public Read(ActiveConnection connection, NetIdBase id) {
            super(connection, id);
        }
    }

    public static class Write extends MessageContext implements IMsgWriteCtx {
        public Write(ActiveConnection connection, NetIdBase id) {
            super(connection, id);
        }
    }
}
