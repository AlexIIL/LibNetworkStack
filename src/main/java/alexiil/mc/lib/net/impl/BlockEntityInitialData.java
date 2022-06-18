/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import java.util.Collection;
import java.util.Collections;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import alexiil.mc.lib.net.mixin.api.IThreadedAnvilChunkStorageMixin;

/** This should be implemented by {@link BlockEntity}s that wish to send their own initial data packet to players
 * individually, rather than using {@link BlockEntity#toInitialChunkDataTag()} or
 * {@link BlockEntity#toUpdatePacket()}. */
public interface BlockEntityInitialData {
    void sendInitialData(ServerPlayerEntity to);

    /** Like {@link #getPlayersWatching(ServerWorld, BlockPos)}, this only returns any players if the chunk containing
     * the block entity has already had the initial data sent from the server. */
    public static Collection<ServerPlayerEntity> getPlayersWatching(BlockEntity be) {
        if (!(be.getWorld() instanceof ServerWorld svWorld)) {
            throw new IllegalArgumentException("Players can only watch server-sided block entities!");
        }
        return getPlayersWatching(svWorld, be.getPos());
    }

    /** This only returns any players if the chunk containing the block entity has already had the initial data sent
     * from the server. */
    public static Collection<ServerPlayerEntity> getPlayersWatching(ServerWorld svWorld, BlockPos pos) {
        // BUGFIX: for some reason minecraft occasionally ticks block entities with players in range
        // But *hasn't* sent the initial chunk data yet, since it hasn't technically finished loading?
        ThreadedAnvilChunkStorage tacs = svWorld.getChunkManager().threadedAnvilChunkStorage;
        IThreadedAnvilChunkStorageMixin mixinTacs = (IThreadedAnvilChunkStorageMixin) tacs;
        ChunkHolder chunkHolder = mixinTacs.libnetworkstack_getChunkHolder(new ChunkPos(pos));
        if (chunkHolder.getWorldChunk() == null) {
            return Collections.emptyList();
        }
        // END BUGFIX

        return PlayerLookup.tracking(svWorld, pos);
    }
}
