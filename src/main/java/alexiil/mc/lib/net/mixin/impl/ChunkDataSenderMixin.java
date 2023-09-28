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
import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import alexiil.mc.lib.net.impl.BlockEntityInitialData;

@Mixin(ChunkDataSender.class)
public class ChunkDataSenderMixin {
    @Inject(method = "sendChunkData", at = @At("RETURN"))
    private static void libnetworkstack_postSendPackets(ServerPlayNetworkHandler handler, ServerWorld world,
                                                        WorldChunk chunk, CallbackInfo ci) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof BlockEntityInitialData initialData) {
                initialData.sendInitialData(handler.player);
            }
        }
    }
}
