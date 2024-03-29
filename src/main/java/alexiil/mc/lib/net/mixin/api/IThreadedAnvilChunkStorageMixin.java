/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;

import alexiil.mc.lib.net.InternalMsgUtil;

/** Used by {@link InternalMsgUtil}.createAndSendDebugThrowable to debug {@link BlockEntity} datas sent before their
 * chunks are sent. */
public interface IThreadedAnvilChunkStorageMixin {
    ChunkHolder libnetworkstack_getChunkHolder(ChunkPos chunkPos);
}
