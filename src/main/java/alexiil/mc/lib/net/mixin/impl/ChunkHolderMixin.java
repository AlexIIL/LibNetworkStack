/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.net.impl.BlockEntityInitialData;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

    private static final String LIST = "Ljava/util/List;";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;";

    @Inject(at = @At("RETURN"), method = "sendBlockEntityUpdatePacket(" + LIST + WORLD + BLOCK_POS + ")V",
        locals = LocalCapture.CAPTURE_FAILHARD)
    void sendCustomUpdatePacket(List<ServerPlayerEntity> players, World world, BlockPos pos, CallbackInfo ci,
                                BlockEntity be) {

        if (be instanceof BlockEntityInitialData dataBe) {
            for (ServerPlayerEntity player : players) {
                dataBe.sendInitialData(player);
            }
        }
    }
}
