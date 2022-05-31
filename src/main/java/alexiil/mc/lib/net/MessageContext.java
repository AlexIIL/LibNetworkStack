/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import net.minecraft.block.entity.BlockEntity;

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

        /** A non-null value indicates that the read message was dropped. */
        public String dropReason;

        public Read(ActiveConnection connection, NetIdBase id) {
            super(connection, id);
        }

        @Override
        public void drop(String reason) {
            if (reason == null) {
                reason = "";
            }
            dropReason = reason;
        }
    }

    public static class Write extends MessageContext implements IMsgWriteCtx {

        // begin TMP_FIX_BLANKETCON_2022_ALEX_01
        /** @deprecated WARNING: Temporary field, used solely to debug an issue. DO NOT TOUCH FROM THIS FIELD IF YOU
         *             AREN'T LIB NETWORK STACK! */
        @Deprecated
        public BlockEntity blockEntity;
        // end TMP_FIX_BLANKETCON_2022_ALEX_01

        public Write(ActiveConnection connection, NetIdBase id) {
            super(connection, id);
        }
    }
}
