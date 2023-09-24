/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;

import alexiil.mc.lib.net.mixin.api.IThreadedAnvilChunkStorageMixin;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin implements IThreadedAnvilChunkStorageMixin {

    @Shadow
    private ChunkHolder getChunkHolder(long pos) {
        throw new Error("Shadow mixin failed to apply!");
    }

    @Override
    public ChunkHolder libnetworkstack_getChunkHolder(ChunkPos chunkPos) {
        return getChunkHolder(chunkPos.toLong());
    }
}
