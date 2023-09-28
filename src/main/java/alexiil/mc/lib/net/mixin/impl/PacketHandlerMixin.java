/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.Packet;

import alexiil.mc.lib.net.impl.IPacketCustomId;
import alexiil.mc.lib.net.mixin.api.IInternalPacketHandlerMixin;
import alexiil.mc.lib.net.mixin.api.IPacketHandlerMixin;

@Mixin(NetworkState.PacketHandler.class)
public class PacketHandlerMixin implements IPacketHandlerMixin {

    @Shadow
    @Final
    private NetworkState.InternalPacketHandler<?> backingHandler;

    @Override
    public IInternalPacketHandlerMixin libnetworkstack_getBackingHandler() {
        return (IInternalPacketHandlerMixin) backingHandler;
    }

    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    private void getId(Packet<?> packet, CallbackInfoReturnable<Integer> cir) {
        if (packet instanceof IPacketCustomId<?> pkt) {
            cir.setReturnValue(pkt.getReadId());
        }
    }
}
