/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import java.util.Map;
import java.util.function.Function;

import alexiil.mc.lib.net.mixin.api.IInternalPacketHandlerMixin;

import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.Packet;

import alexiil.mc.lib.net.impl.IPacketCustomId;
import alexiil.mc.lib.net.mixin.api.INetworkStateMixin;
import alexiil.mc.lib.net.mixin.api.IPacketHandlerMixin;

@Mixin(NetworkState.class)
public class NetworkStateMixin implements INetworkStateMixin {

    @Final
    @Shadow
    private Map<NetworkSide, ? extends IPacketHandlerMixin> packetHandlers;

    @Override
    public <P extends IPacketCustomId<?>> int libnetworkstack_registerPacket(NetworkSide recvSide, Class<P> klass, Function<PacketByteBuf, P> factory) {
        IInternalPacketHandlerMixin mapping = packetHandlers.get(recvSide).libnetworkstack_getBackingHandler();
        return mapping.libnetworkstack_register(klass, factory);
    }
}
