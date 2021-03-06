/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;

import alexiil.mc.lib.net.impl.BlockEntityInitialData;
import alexiil.mc.lib.net.mixin.api.IBlockEntityInitialData;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Inject(at = @At("RETURN"), method = "sendChunkDataPackets(Lnet/minecraft/server/network/ServerPlayerEntity;"
        + "[Lnet/minecraft/network/Packet;Lnet/minecraft/world/chunk/WorldChunk;)V")
    private void postSendPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo ci) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof BlockEntityInitialData) {
                ((BlockEntityInitialData) be).sendInitialData(player);
            }
        }
    }
}
