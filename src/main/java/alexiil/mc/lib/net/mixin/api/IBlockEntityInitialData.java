/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import alexiil.mc.lib.net.impl.BlockEntityInitialData;

/** This should be implemented by {@link BlockEntity}s that wish to send their own initial data packet to players
 * individually, rather than using {@link BlockEntity#toInitialChunkDataTag()} or {@link BlockEntity#toUpdatePacket()}.
 * 
 * @deprecated This has been renamed to {@link BlockEntityInitialData}, and moved out of this mixin package and into the
 *             "impl" package. (Although implementing this still works). */
@Deprecated
public interface IBlockEntityInitialData extends BlockEntityInitialData {
    @Override
    void sendInitialData(ServerPlayerEntity to);
}
