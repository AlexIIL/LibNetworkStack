/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import java.util.function.Function;

import net.minecraft.network.NetworkSide;

import alexiil.mc.lib.net.impl.IPacketCustomId;
import net.minecraft.network.PacketByteBuf;

public interface INetworkStateMixin {
    <P extends IPacketCustomId<?>> int libnetworkstack_registerPacket(NetworkSide recvSide, Class<P> klass, Function<PacketByteBuf, P> factory);
}
