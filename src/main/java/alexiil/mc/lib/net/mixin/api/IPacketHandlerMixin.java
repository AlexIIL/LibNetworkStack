/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import java.util.function.Supplier;

import net.minecraft.network.Packet;

public interface IPacketHandlerMixin {
    int libnetworkstack_register(Class<? extends Packet<?>> klass, Supplier<? extends Packet<?>> factory);
}
