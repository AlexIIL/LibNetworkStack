/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import java.util.List;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.ByteBuf;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.network.handler.PacketCodecDispatcher;
import net.minecraft.network.packet.PacketType;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.IPacketCustomId;

@Mixin(PacketCodecDispatcher.class)
public class PacketCodecDispatcherMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Function<?, ?> packetIdGetter, List<?> packetTypes, Object2IntMap<PacketType<?>> typeToIndex, CallbackInfo ci) {
        CoreMinecraftNetUtil.setExpectedIds(typeToIndex);
    }
    
    // Mixes into PacketCodecDispatcher to allow packets to encode themselves with custom integer ids
    @ModifyExpressionValue(method = "encode(Lio/netty/buffer/ByteBuf;Ljava/lang/Object;)V", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap;getOrDefault(Ljava/lang/Object;I)I"))
    private int getCustomPacketId(int original, ByteBuf buf_, Object packet) {
        if (packet instanceof IPacketCustomId<?> customId) {
            return customId.getReadId();
        }
        return original;
    }
}
