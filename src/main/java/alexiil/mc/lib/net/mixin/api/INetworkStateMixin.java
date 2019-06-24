/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import net.minecraft.network.NetworkSide;

import alexiil.mc.lib.net.impl.IPacketCustomId;

public interface INetworkStateMixin {
    int libnetworkstack_registerPacket(NetworkSide recvSide, Class<? extends IPacketCustomId<?>> packet);
}
