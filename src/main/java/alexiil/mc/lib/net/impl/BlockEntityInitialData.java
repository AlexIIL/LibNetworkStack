/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** This should be implemented by {@link BlockEntity}s that wish to send their own initial data packet to players
 * individually, rather than using {@link BlockEntity#toInitialChunkDataTag()} or
 * {@link BlockEntity#toUpdatePacket()}. */
public interface BlockEntityInitialData {
    void sendInitialData(ServerPlayerEntity to);
}
