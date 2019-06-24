/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import net.minecraft.network.NetworkSide;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

public interface IMsgCtx {

    ActiveConnection getConnection();

    /** @return The {@link NetworkSide} for this {@link #getConnection()}, assuming that it is an
     *         {@link ActiveMinecraftConnection}. */
    default EnumNetSide getNetSide() {
        ActiveConnection connection = getConnection();
        return ((ActiveMinecraftConnection) connection).getNetSide();
    }

    /** @return true if the code calling this is running as the master/server. (In normal connections the dedicated or
     *         integrated server is the master, and the client is the slave). Assumes that the connection is an
     *         {@link ActiveMinecraftConnection}. */
    default boolean isServerSide() {
        return getNetSide() == EnumNetSide.SERVER;
    }

    /** @return true if the code calling this is running as the slave/client. (In normal connections the dedicated or
     *         integrated server is the master, and the client is the slave). Assumes that the connection is an
     *         {@link ActiveMinecraftConnection}. */
    default boolean isClientSide() {
        return getNetSide() == EnumNetSide.CLIENT;
    }

    NetIdBase getNetId();
}
