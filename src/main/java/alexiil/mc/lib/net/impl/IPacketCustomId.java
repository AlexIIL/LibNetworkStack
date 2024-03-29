/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;

public interface IPacketCustomId<T extends PacketListener> extends Packet<T> {
    /** @return The integer ID that the other side is expecting. */
    int getReadId();
}
